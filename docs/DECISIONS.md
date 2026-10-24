# Décisions d’architecture — e-KYC & Scoring

Ce document consigne les choix structurants, les écarts assumés par rapport au cahier des
charges et les conséquences opérationnelles. Il est destiné à être mis à jour à chaque évolution
significative.

---

## ADR-001 — Spring Boot 3.5 plutôt que Spring Boot 4

**Contexte.** Le cahier des charges exige Java 21 et Spring Boot 3.3+. Spring Boot 4 était
disponible mais implique Spring Security 7 et plusieurs ruptures d’API.

**Décision.** Utiliser **Spring Boot 3.5.16** (branche 3.x supportée), Spring Security 6, Java 21.

**Conséquences.** API stables, écosystème Testcontainers/springdoc compatible, montée de version
ultérieure à planifier avec une campagne de tests dédiée. Aucun impact fonctionnel.

---

## ADR-002 — Table `refresh_token` ajoutée au schéma

**Contexte.** Le schéma fourni ne contient pas de table pour les refresh tokens, alors que
SEC-02 exige une rotation et une révocation.

**Décision.** Ajouter la table `refresh_token` dans `V1__kyc.sql` :

- `token_hash` (SHA-256, `CHAR(64)`) — le jeton en clair n’est **jamais** persisté ;
- `expire_le`, `revoque_le`, `remplace_par` pour la rotation et la détection de réutilisation ;
- index sur `token_hash` et `utilisateur_id`.

**Conséquences.** Un refresh token réutilisé révoque tous les jetons actifs de l’utilisateur
(`TOKEN_REUTILISE` dans l’audit). Le champ `tokenClair` de l’entité est `@Transient`.

---

## ADR-003 — Migration `V0` minimale pour les dépendances métier

**Contexte.** Le schéma cible référence `client`, `credit`, `echeance` et `agence` sans les créer,
et le projet part d’une base vide.

**Décision.** `V0__base_minimale.sql` crée les tables socles minimales (`agence`, `utilisateur`,
`client`, `credit`, `echeance`) nécessaires aux clés étrangères et au calcul de l’historique de
remboursement. `V1__kyc.sql` ajoute le périmètre KYC, `V2__immutabilite.sql` les triggers et
privilèges, `V3__seed.sql` les données de démonstration.

**Conséquences.** Le modèle socle est volontairement réduit ; il devra être aligné sur le SI
existant lors de l’intégration réelle (colonnes, contraintes, référentiels).

---

## ADR-004 — Deux endpoints de lecture ajoutés

**Contexte.** Les écrans Angular ont besoin de listes non prévues explicitement dans le cahier des
charges :

- le formulaire agent doit choisir un client de son agence ;
- l’écran d’administration doit choisir une agence.

**Décision.** Ajouter :

- `GET /kyc/clients` — réservé à `AGENT_ACCUEIL`, retourne uniquement les clients de son agence ;
- `GET /admin/agences` — réservé à `ADMIN`.

**Conséquences.** Surface API légèrement élargie, sans donner accès à des données hors périmètre.
À documenter dans la spécification OpenAPI et à intégrer au catalogue d’API de l’institution.

---

## ADR-005 — Correction d’un dossier rejeté = nouvelle version

**Contexte.** Le workflow autorise la correction après rejet, mais l’historique doit rester
immuable.

**Décision.** `PUT /kyc/dossiers/{id}` sur un dossier `REJETE` crée une **nouvelle ligne**
`dossier_kyc` avec `version = version_max(client) + 1`, statut `BROUILLON`. Le dossier rejeté reste
consultable. Les documents ne sont **pas copiés** : ils doivent être redéposés, ce qui garantit
que la nouvelle version repose sur des pièces vérifiées et évite de propager une pièce compromise.

**Conséquences.** L’agent est averti dans l’interface ; le nouveau dossier possède son propre
historique d’audit et de décisions. La contrainte `UNIQUE (client_id, version)` protège contre les
doublons.

---

## ADR-006 — Matrice de permissions

**Décision.**

| Action | Agent | Analyste | Superviseur | Admin |
| --- | --- | --- | --- | --- |
| Créer / modifier / soumettre un dossier | son agence, dossiers qu’il a créés | — | — | — |
| Déposer un document | son dossier brouillon | — | — | — |
| Lister les dossiers | son agence | tous | tous | — |
| Consulter un dossier | son agence | tous | tous | — |
| Télécharger un document | — | dossier assigné | tous | — |
| Prendre en charge / scorer | — | dossier assigné | — | — |
| Décider | — | — | oui, hors quatre yeux | — |
| Audit / vérification | — | — | oui | — |
| Utilisateurs / critères | — | — | — | oui |

L’admin est volontairement exclu des dossiers KYC (séparation des fonctions). Les contrôles sont
appliqués côté service (`@PreAuthorize` + vérifications objet), pas seulement côté route.

---

## ADR-007 — Scoring explicite, bornes inclusives et données neutres

**Contexte.** Le cahier des charges décrit quatre critères et des tranches paramétrables, sans
préciser le comportement aux bornes ni en l’absence d’historique.

**Décision.**

- Score sur 100 = somme des points des quatre critères (0 à 25 chacun).
- Bornes **inclusives** ; en cas de chevauchement, la tranche active ayant la plus petite
  `borneMin` gagne.
- L’administration refuse les chevauchements de tranches actives (`409 CHEVAUCHEMENT_CRITERES`).
- Absence d’historique de remboursement : **12 points neutres**, traçés comme tels dans le détail.
- Les données de seed utilisent des bornes décalées (`30,0001`, etc.) pour éviter tout
  chevauchement avec des bornes inclusives.
- Paliers : `FAVORABLE` ≥ 70, `A_ETUDIER` 50–69, `DEFAVORABLE` < 50.
- Le calcul exige que l’analyste soit assigné au dossier et que le dossier soit `EN_REVUE`.
- Chaque calcul crée un `resultat_scoring` **et** une ligne `decision_kyc` (`ANALYSE`,
  `RECOMMANDE`) pour que l’historique métier soit complet.

**Conséquences.** Le score est reproductible et auditable ; il reste une aide, jamais une décision
automatique.

---

## ADR-008 — Principe des quatre yeux

**Décision.** Un superviseur ne peut pas rendre la décision finale s’il est :

- l’agent ayant soumis le dossier (`soumis_par`), ou
- l’analyste assigné (`assigne_a`).

Le refus renvoie `403 QUATRE_YEUX` et écrit un `ACCES_REFUSE`. La règle est vérifiée dans le
service, donc indépendamment de l’interface. Les rôles étant exclusifs dans le modèle actuel, le
contrôle protège surtout contre des comptes multi-rôles ou des évolutions futures.

**Conséquences.** La séparation des tâches est démontrable en audit ; un superviseur ne peut pas
« valider son propre travail ».

---

## ADR-009 — Chaîne d’audit et sérialisation des écritures

**Décision.**

- Chaque ligne contient `hash_precedent` et `hash = SHA-256(hash_precedent + canonique)`.
- La forme canonique trie les clés JSON et préfixe chaque champ par sa longueur, afin d’éviter
  les collisions de concaténation et de rendre la vérification indépendante de l’ordre des clés.
- `pg_advisory_xact_lock(742001)` sérialise les insertions concurrentes ; le verrou est relâché à
  la fin de la transaction.
- Les triggers PostgreSQL interdisent `UPDATE`, `DELETE` et `TRUNCATE` sur `audit_log` et
  `decision_kyc`.
- Le rôle `kyc_app` ne reçoit que `SELECT, INSERT` sur ces deux tables (voir ADR-011).
- Les refus d’accès sont écrits dans une transaction séparée (`REQUIRES_NEW`) pour ne pas être
  perdus si l’action métier est annulée.

**Conséquences.** Toute altération est détectable, mais la vérification est **locale** : un
attaquant disposant d’un accès superutilisateur pourrait recalculer toute la chaîne. Une
externalisation périodique des ancres de hash est recommandée (voir README, évolutions).

---

## ADR-010 — Stockage des jetons côté navigateur

**Décision.** Les jetons sont conservés en `sessionStorage`, jamais en `localStorage`. L’access
token est court (15 min) et renouvelé automatiquement ; le refresh token est opaque, haché en base
et tourné à chaque usage. L’intercepteur Angular ajoute le Bearer, tente un refresh sur `401` et
déconnecte si le refresh échoue.

**Conséquences.** La session ne survit pas à la fermeture de l’onglet. Un XSS reste critique :
la CSP, l’absence de `innerHTML` non maîtrisé et la revue des dépendances sont indispensables. Une
alternative plus robuste serait un cookie `HttpOnly` + protection CSRF, à étudier si le frontend
et l’API sont servis sur la même origine (c’est le cas en Docker).

---

## ADR-011 — Rôles PostgreSQL séparés

**Décision.**

- `kyc` : propriétaire de la base, utilisé uniquement par Flyway (DDL).
- `kyc_app` : rôle applicatif, `SELECT/INSERT/UPDATE/DELETE` sur les tables métier,
  `SELECT/INSERT` seulement sur `audit_log` et `decision_kyc`.
- Les rôles sont créés par `docker/postgres/init/01-roles.sh` avant l’exécution des migrations.
- `V2` applique les privilèges si le rôle existe ; les privilèges par défaut sont posés pour les
  futures tables.

**Conséquences.** Une injection SQL dans l’application ne permet ni de modifier ni de supprimer
les preuves d’audit. Les sauvegardes et tâches d’administration doivent utiliser un rôle distinct,
jamais `kyc_app`.

---

## ADR-012 — Tests d’intégration : Testcontainers avec repli embarqué

**Contexte.** Les tests doivent tourner sur un PostgreSQL réel, mais le démon Docker peut être
indisponible sur un poste de développement.

**Décision.** `AbstractIntegrationTest` tente d’abord Testcontainers (`postgres:16-alpine`), puis
bascule sur un PostgreSQL embarqué (Zonky) si Docker est inaccessible. Les migrations et le schéma
sont identiques dans les deux cas.

**Conséquences.** Les tests restent exécutables partout, au prix d’une dépendance de test
supplémentaire. En CI, Docker doit être disponible pour valider le chemin nominal.

---

## ADR-013 — Contexte HTTP `/api`

**Décision.** Le backend expose toutes ses routes sous `/api`
(`server.servlet.context-path=/api`). Le proxy de développement Angular et nginx transmettent le
préfixe sans réécriture. Les tests MockMvc appellent les chemins sans `/api`, le contexte servlet
n’étant pas appliqué au `MockServletContext`.

**Conséquences.** Une seule origine en production (`https://kyc.example`), donc pas de CORS en
Docker. En développement direct, `CORS_ORIGINS` doit contenir `http://localhost:4200`.

---

## ADR-014 — Angular standalone, Signals et Material

**Décision.** Application Angular 22 standalone, sans `NgModule`, avec :

- `provideHttpClient(withInterceptors([authInterceptor]))` ;
- routes protégées par `authGuard` / `roleGuard` / `accueilGuard` ;
- état local par `signal` / `computed` ;
- formulaires réactifs typés ;
- Angular Material pour les composants de structure (tables, dialogues, formulaires) ;
- services `core/` sans état métier global, les pages portant l’état de leur écran ;
- routes chargées paresseusement via `loadComponent` (y compris les écrans d’un même rôle).

**Conséquences.** Code concis et testable, pas de store global. Le chargement paresseux contient le
bundle initial à 422.79 kB brut (107.39 kB transféré), sous le budget d’avertissement de 500 kB
défini dans `angular.json` ; le build de production ne produit plus d’avertissement de taille.

---

## ADR-015 — Masquage et affichage du numéro de pièce

**Décision.** Le numéro est masqué par défaut (`TG****1234` : deux premiers caractères et quatre
derniers). L’agent ne voit que la version masquée ; l’analyste et le superviseur reçoivent le
numéro en clair dans le détail du dossier. Les listes et l’audit ne contiennent jamais le numéro
en clair.

**Conséquences.** Réduction de l’exposition dans les écrans et les exports ; le besoin métier de
saisie reste couvert côté agent par le formulaire.

---

## ADR-016 — Pas de suppression des documents ni des dossiers

**Décision.** Aucun endpoint de suppression n’est exposé pour les dossiers et documents. Le
remplacement d’une pièce crée une nouvelle ligne et renseigne `remplace_par` sur l’ancienne. La
désactivation d’un utilisateur est logique (`actif = false`).

**Conséquences.** L’historique et les preuves sont préservés. La purge RGPD devra être un
processus d’administration explicitement tracé, distinct de l’API métier.

---

## ADR-017 — Format d’erreur uniforme

**Décision.** Utiliser `ProblemDetail` (`application/problem+json`) avec un champ `code` stable
(`AUTHENTIFICATION_ECHOUEE`, `QUATRE_YEUX`, `DOCUMENT_ALTERE`, etc.) et, pour les erreurs de
validation, un tableau `erreurs[{champ, message}]`. Les messages internes et les stack traces ne
sont jamais renvoyés.

**Conséquences.** Le frontend peut afficher un message métier fiable et les tests peuvent asserter
sur les codes plutôt que sur les libellés.

---

## ADR-018 — Choix de déploiement

**Décision.** Trois conteneurs : PostgreSQL 16, backend Java 21, frontend nginx. Le backend est
construit en multi-étapes (Maven puis JRE Alpine) et s’exécute avec un utilisateur non privilégié.
Le frontend est un build statique servi par nginx, qui joue aussi le rôle de reverse proxy `/api`.

**Conséquences.** Une seule origine, CSP homogène, pas de CORS en production. TLS, sauvegardes et
supervision restent à la charge de la plateforme d’hébergement.

---

## Points ouverts

1. **MFA** pour les rôles superviseur et admin (TOTP/WebAuthn).
2. **OCR et détection de falsification** des pièces, capture mobile et vivacité.
3. **Antivirus** sur les uploads et analyse des PDF.
4. **Rate limiting** global et par utilisateur au reverse proxy.
5. **KMS/HSM** et rotation des clés de chiffrement ; procédure de ré-chiffrement.
6. **Sauvegardes chiffrées**, PITR et exercices de restauration.
7. **Ancrage externe** de la chaîne d’audit (horodatage qualifié).
8. **Purge et anonymisation** encadrées juridiquement, avec journalisation.
9. **Préchargement** sélectif des routes Angular et suivi du budget de bundle au fil des ajouts.
10. **Accessibilité** : audit WCAG/RGAA et tests lecteur d’écran.
