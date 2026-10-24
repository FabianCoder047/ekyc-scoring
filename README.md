# e-KYC & Scoring — SFD

Application interne de **connaissance client (e-KYC)** et de **scoring** pour les dossiers de
crédit d’une institution de microfinance. Elle couvre le parcours complet : création du dossier
par une agence, dépôt sécurisé des pièces d’identité, instruction par un analyste, calcul d’un
score explicable, décision finale par un superviseur selon le **principe des quatre yeux**, et
traçabilité intégrale dans un **journal d’audit chaîné**.

> Ce dépôt contient une application de démonstration. Les comptes et mots de passe fournis sont
> publics et ne doivent jamais être utilisés en production.

---

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Démarrage rapide avec Docker](#démarrage-rapide-avec-docker)
- [Développement local](#développement-local)
- [Configuration](#configuration)
- [Comptes de démonstration](#comptes-de-démonstration)
- [Parcours et règles métier](#parcours-et-règles-métier)
- [API](#api)
- [Sécurité](#sécurité)
- [Modèle de menace](#modèle-de-menace)
- [Protection des données et conformité](#protection-des-données-et-conformité)
- [Tests](#tests)
- [Structure du dépôt](#structure-du-dépôt)
- [Limites connues et évolutions](#limites-connues-et-évolutions)
- [Dépannage](#dépannage)

---

## Fonctionnalités

- **Gestion des dossiers KYC** : brouillon, soumission, prise en charge, revue, validation ou
  rejet motivé. Correction d’un dossier rejeté par création d’une nouvelle version.
- **RBAC** : `AGENT_ACCUEIL`, `ANALYSTE_KYC`, `SUPERVISEUR`, `ADMIN`, avec cloisonnement par
  agence et restrictions par dossier.
- **Pièces d’identité** : upload JPEG/PNG/PDF (5 Mo max), détection du type réel par Apache Tika,
  chiffrement **AES-256-GCM**, empreinte SHA-256 vérifiée à chaque lecture, stockage hors base.
- **Scoring explicable** : règles paramétrables par tranches, score sur 100, détail par critère,
  historique des calculs.
- **Décision humaine** : le score est une aide ; la décision finale est prise par un superviseur
  qui ne peut être ni l’agent ayant soumis le dossier, ni l’analyste l’ayant instruit.
- **Audit chaîné** : chaque action sensible est horodatée et chaînée par SHA-256 ; un endpoint
  permet de vérifier l’intégrité de toute la chaîne.
- **Masquage** des numéros de pièce dans les listes et les traces d’audit.
- **Frontend Angular** standalone avec Signals, formulaires réactifs et Angular Material.

## Architecture

```
┌─────────────────────┐        HTTP /api        ┌──────────────────────────┐
│  Navigateur         │ ──────────────────────► │  nginx (frontend)        │
│  Angular 22         │ ◄────────────────────── │  SPA + reverse proxy     │
└─────────────────────┘                         └───────────┬──────────────┘
                                                            │ /api
                                                ┌───────────▼──────────────┐
                                                │  Spring Boot 3.5 / Java 21│
                                                │  JWT · RBAC · audit      │
                                                └───────┬──────────┬───────┘
                                                        │          │
                                          ┌─────────────▼──┐   ┌───▼────────────────┐
                                          │ PostgreSQL 16  │   │ Volume chiffré     │
                                          │ Flyway         │   │ documents (AES)    │
                                          └────────────────┘   └────────────────────┘
```

- `backend/` : API REST Spring Boot, package `com.sfd.kyc`.
- `frontend/` : application Angular (build statique servi par nginx).
- `docker/postgres/init/` : création des rôles PostgreSQL `kyc` (migrations) et `kyc_app`
  (application, privilèges limités).
- `docs/DECISIONS.md` : décisions d’architecture et écarts assumés par rapport au cahier des
  charges.

## Démarrage rapide avec Docker

### Prérequis

- Docker Engine 24+ et Docker Compose v2
- 4 Go de RAM disponibles (PostgreSQL + build Maven + build Node)

### 1. Configurer les secrets

```bash
cp .env.example .env
```

Renseigner ensuite `.env` :

```bash
# Mot de passe du superutilisateur PostgreSQL
openssl rand -base64 24

# Secrets applicatifs
openssl rand -base64 48   # JWT_SECRET (>= 32 octets)
openssl rand -base64 32   # KYC_ENCRYPTION_KEY (exactement 32 octets)
```

`KYC_ENCRYPTION_KEY` doit décoder exactement **32 octets** ; `JWT_SECRET` au moins **32 octets**.
Les deux sont attendus en base64. Ne jamais versionner `.env`.

### 2. Lancer la pile

```bash
docker compose up --build
```

| Service | URL |
| --- | --- |
| Application | <http://localhost:8081> |
| API | <http://localhost:8080/api> |
| Documentation OpenAPI | <http://localhost:8080/api/swagger-ui.html> |

Le premier démarrage applique les migrations Flyway (`V0` à `V3`) et crée les données de
démonstration. Le volume `postgres-data` conserve la base ; `kyc-documents` conserve les pièces
chiffrées.

Pour arrêter et **effacer** les données de démonstration :

```bash
docker compose down -v
```

> Les scripts d’initialisation PostgreSQL ne s’exécutent qu’à la création du volume. Si les mots
> de passe changent dans `.env` après le premier démarrage, mettre à jour les rôles manuellement
> (voir [Dépannage](#dépannage)) ou recréer le volume.

## Développement local

### Backend

Prérequis : Java 21, Maven 3.9+ (le wrapper `mvnw` est fourni), PostgreSQL 16.

Créer la base et les rôles (une seule fois) :

```sql
CREATE ROLE kyc LOGIN PASSWORD 'kyc';
CREATE ROLE kyc_app LOGIN PASSWORD 'kyc_app';
CREATE DATABASE kyc OWNER kyc;
\c kyc
GRANT USAGE, CREATE ON SCHEMA public TO kyc;
GRANT USAGE ON SCHEMA public TO kyc_app;
```

Puis :

```bash
cd backend
export DB_URL=jdbc:postgresql://localhost:5432/kyc
export DB_APP_USER=kyc_app
export DB_APP_PASSWORD=kyc_app
export DB_MIGRATION_USER=kyc
export DB_MIGRATION_PASSWORD=kyc
export JWT_SECRET="$(openssl rand -base64 48)"
export KYC_ENCRYPTION_KEY="$(openssl rand -base64 32)"
./mvnw spring-boot:run
```

L’API écoute sur <http://localhost:8080/api>.

### Frontend

```bash
cd frontend
npm ci
npm start
```

Le serveur de développement Angular écoute sur <http://localhost:4200> et proxyfie `/api` vers
`http://localhost:8080` (`proxy.conf.json`). Le backend doit autoriser cette origine via
`CORS_ORIGINS` (valeur par défaut : `http://localhost:4200`).

### Sans Docker pour PostgreSQL

Si seul le backend doit tourner et que Docker n’est pas disponible, utiliser une instance
PostgreSQL 16 locale avec les rôles ci-dessus. Les tests d’intégration, eux, savent démarrer un
PostgreSQL embarqué lorsque le démon Docker est inaccessible.

## Configuration

Toutes les valeurs sensibles sont fournies par variables d’environnement.

| Variable | Obligatoire | Description |
| --- | --- | --- |
| `DB_URL` | non | URL JDBC (défaut `jdbc:postgresql://localhost:5432/kyc`) |
| `DB_APP_USER` / `DB_APP_PASSWORD` | non | Rôle applicatif (défaut `kyc_app`) |
| `DB_MIGRATION_USER` / `DB_MIGRATION_PASSWORD` | non | Rôle Flyway (défaut `kyc`) |
| `JWT_SECRET` | **oui** | Secret HMAC base64, ≥ 32 octets |
| `KYC_ENCRYPTION_KEY` | **oui** | Clé AES-256 base64, exactement 32 octets |
| `KYC_STORAGE_PATH` | non | Répertoire des documents chiffrés (défaut `./data/documents`) |
| `CORS_ORIGINS` | non | Origines autorisées, séparées par des virgules |
| `POSTGRES_PASSWORD` | Docker | Mot de passe du superutilisateur PostgreSQL |
| `FRONTEND_PORT` / `BACKEND_PORT` | non | Ports publiés par Compose (défaut 8081 / 8080) |

Autres réglages dans `backend/src/main/resources/application.yml` : durée des jetons (15 min /
7 jours), verrouillage après 5 échecs sur 15 minutes, taille maximale des fichiers (5 Mo),
`ddl-auto=validate`, fuseau UTC.

## Comptes de démonstration

Mot de passe commun : **`Demo123456!`**

| Rôle | Email | Usage |
| --- | --- | --- |
| Agent d’accueil | `agent@sfd.tg` | Crée les dossiers, dépose les pièces, soumet |
| Analyste KYC | `analyste@sfd.tg` | Prend en charge, consulte les pièces, calcule le score |
| Superviseur | `superviseur@sfd.tg` | Décide, consulte l’audit et vérifie la chaîne |
| Administrateur | `admin@sfd.tg` | Gère utilisateurs, agences et critères de scoring |

Données de démonstration : agences `LOME-CENTRE` et `KARA`, clients `+22890000001` (historique de
remboursement avec 1 retard sur 10), `+22890000002` (sans historique), `+22890000003` (agence de
Kara). Le client 2 est adapté au scénario de scoring d’acceptation.

## Parcours et règles métier

```
BROUILLON ──soumission──► SOUMIS ──prise en charge──► EN_REVUE ──décision──► VALIDE
     ▲                                                      │
     └──────────── correction (nouvelle version) ◄── REJETE ┘
```

1. **Agent** : crée un dossier pour un client de son agence, renseigne la pièce et la situation
   financière, dépose recto et verso, puis soumet. Un dossier expiré ou sans recto/verso est
   refusé.
2. **Analyste** : prend en charge un dossier `SOUMIS` (il devient `EN_REVUE` et lui est assigné),
   consulte les documents, calcule le scoring.
3. **Superviseur** : valide ou rejette un dossier `EN_REVUE`. Le rejet exige un motif. Le
   superviseur ne peut pas être l’agent ayant soumis ni l’analyste assigné (quatre yeux).
4. **Correction** : un dossier `REJETE` peut être corrigé par son agent ; l’enregistrement crée
   une nouvelle version (`version + 1`) et les documents doivent être redéposés.

### Scoring

Le score est la somme de quatre critères, chacun noté de 0 à 25 points :

| Critère | Mesure |
| --- | --- |
| `RATIO_ENDETTEMENT` | `charges / revenu × 100`, en % |
| `ANCIENNETE_ACTIVITE` | ancienneté en mois |
| `REVENU_MENSUEL` | revenu mensuel en FCFA |
| `HISTORIQUE_REMBOURSEMENT` | % d’échéances en retard ; **12 points neutres** si aucun historique |

Règles : bornes **inclusives**, première tranche active par `borneMin` croissante, tranches
paramétrables par l’administrateur. Paliers : `FAVORABLE` (≥ 70), `A_ETUDIER` (50–69),
`DEFAVORABLE` (< 50). Le score ne déclenche jamais automatiquement une décision.

## API

Préfixe : `/api`. Toutes les réponses d’erreur suivent le format `application/problem+json`
(`code`, `detail`, éventuellement `erreurs[]`). Les réponses API sont `Cache-Control: no-store`.

| Méthode | Chemin | Rôles | Description |
| --- | --- | --- | --- |
| POST | `/auth/login` | public | Connexion, renvoie access + refresh token |
| POST | `/auth/refresh` | public | Rotation du refresh token |
| POST | `/auth/logout` | authentifié | Révocation du refresh token |
| GET | `/kyc/clients` | agent | Clients de l’agence (formulaire) |
| GET | `/kyc/dossiers` | agent, analyste, superviseur | Liste paginée + filtres |
| POST | `/kyc/dossiers` | agent | Création d’un brouillon |
| GET | `/kyc/dossiers/{id}` | agent, analyste, superviseur | Détail |
| PUT | `/kyc/dossiers/{id}` | agent propriétaire | Modification / correction |
| POST | `/kyc/dossiers/{id}/documents` | agent propriétaire | Dépôt d’une pièce |
| GET | `/kyc/dossiers/{id}/documents/{docId}/contenu` | analyste assigné, superviseur | Téléchargement |
| POST | `/kyc/dossiers/{id}/soumission` | agent propriétaire | Soumission |
| POST | `/kyc/dossiers/{id}/prise-en-charge` | analyste | Assignation |
| POST | `/kyc/dossiers/{id}/scoring` | analyste assigné | Calcul du score |
| POST | `/kyc/dossiers/{id}/decision` | superviseur | Validation / rejet |
| GET | `/kyc/dossiers/{id}/historique` | analyste, superviseur | Décisions + scorings |
| GET | `/audit` | superviseur | Journal paginé + filtres |
| GET | `/audit/verification` | superviseur | Vérification de la chaîne |
| GET/POST/PUT/DELETE | `/admin/utilisateurs` | admin | Gestion des comptes |
| GET | `/admin/agences` | admin | Liste des agences |
| GET/POST/PUT/DELETE | `/admin/criteres-scoring` | admin | Gestion des tranches |

La documentation interactive est disponible sur `/api/swagger-ui.html` (désactivable en
production via `springdoc.*.enabled=false`).

## Sécurité

### Authentification et sessions

- Mots de passe hachés en **BCrypt coût 12**.
- **Access token JWT HS256** de 15 minutes ; **refresh token opaque** de 7 jours.
- Refresh token stocké **haché** (SHA-256) en base, avec **rotation** à chaque usage et détection
  de réutilisation : un jeton déjà tourné révoque toute la famille de jetons de l’utilisateur.
- Verrouillage temporaire après **5 échecs** sur **15 minutes** (HTTP 429).
- Réponse de connexion uniforme et hash factice pour limiter l’énumération des comptes.
- Côté navigateur, les jetons sont conservés en `sessionStorage` (pas de `localStorage`), avec
  renouvellement automatique et déconnexion en cas d’échec.
- Aucune session serveur : API stateless.

### Autorisations

- RBAC par rôle, complété par des contrôles métier : cloisonnement par agence, propriété du
  dossier, assignation de l’analyste, quatre yeux.
- Les refus d’accès (routage et méthode) sont journalisés en `ACCES_REFUSE`.
- Le rôle `ADMIN` ne peut ni consulter les dossiers KYC ni les documents : séparation des
  fonctions.

### Documents

- Détection du type réel par **Apache Tika** ; seuls JPEG, PNG et PDF sont acceptés.
- Taille limitée à 5 Mo (configuration serveur + nginx).
- Chiffrement **AES-256-GCM** avec IV aléatoire de 12 octets et AAD `sfd-kyc-document-v1`.
- Nom de stockage UUID, contrôle anti-traversée de chemin, empreinte SHA-256 vérifiée à chaque
  lecture ; toute incohérence renvoie `409 DOCUMENT_ALTERE`.
- Aucun document n’est exposé par URL publique : le téléchargement passe par l’API authentifiée.

### Audit

- Table `audit_log` en **append-only** (triggers PostgreSQL + privilèges `SELECT, INSERT` pour le
  rôle applicatif).
- Chaînage : `hash = SHA-256(hash_precedent + canonique)`, verrou consultatif
  `pg_advisory_xact_lock` pour sérialiser les écritures.
- Canonicalisation déterministe (clés JSON triées, champs préfixés par leur longueur) pour que la
  vérification soit reproductible.
- Vérification complète via `/audit/verification` ; toute modification, suppression ou
  troncature est détectée.
- `decision_kyc` est également append-only : l’historique des décisions ne peut pas être réécrit.

### En-têtes et transport

- API : CSP restrictive, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`,
  `Referrer-Policy: no-referrer`, HSTS, `Cache-Control: no-store`.
- Frontend nginx : CSP, `Permissions-Policy`, cache des ressources fingerprintées.
- En production, terminer TLS en amont (reverse proxy / ingress) et ne jamais exposer
  PostgreSQL.

## Modèle de menace

Périmètre : application web interne, API, base PostgreSQL, volume de documents, chaîne de
déploiement. Les acteurs considérés sont l’utilisateur légitime, l’utilisateur interne malveillant
ou compromis, un attaquant externe et un administrateur système.

| # | Menace | Vecteur | Impact | Mesures en place | Risque résiduel |
| --- | --- | --- | --- | --- | --- |
| T1 | Vol de session | XSS, poste compromis, vol de jeton | Usurpation d’un agent/analyste | CSP, `no-store`, access token 15 min, refresh rotation, `sessionStorage`, déconnexion sur réutilisation | XSS zero-day ou poste compromis ; MFA à ajouter |
| T2 | Rejeu du refresh token | Interception réseau ou vol | Session prolongée | TLS attendu, jeton haché en base, rotation, révocation de famille | Vol simultané avant rotation ; détection a posteriori |
| T3 | Force brute / credential stuffing | Formulaire de connexion | Prise de compte | BCrypt 12, verrouillage 5/15 min, audit des échecs, hash factice | Attaque distribuée sur de nombreux comptes ; MFA et CAPTCHA à ajouter |
| T4 | Élévation de privilèges | Appels API forgés, IDOR | Accès à des dossiers d’autres agences | RBAC + contrôles objet côté service, audit des refus, admin sans accès KYC | Erreur de règle métier ; tests de sécurité à maintenir |
| T5 | Contournement des quatre yeux | Compte multi-rôles ou collision | Validation frauduleuse | Contrôle serveur sur soumetteur et analyste assigné ; refus audité | Comptes partagés ; politique RH et MFA |
| T6 | Fuite de pièces d’identité | Accès au volume, sauvegarde, lecture non autorisée | Atteinte à la vie privée | AES-256-GCM, clé hors base, SHA-256, accès par rôle/assignation, audit des consultations | Compromission de la clé ou de l’hôte ; KMS/HSM à prévoir |
| T7 | Fichier malveillant | Upload déguisé, bombe de décompression | Exécution, déni de service | Allowlist Tika, taille 5 Mo, stockage hors webroot, jamais servi directement | Analyse antivirus absente ; ajouter ClamAV |
| T8 | Falsification de l’audit | Accès SQL, suppression de traces | Perte de preuve | Chaîne SHA-256, triggers append-only, privilèges restreints, vérification | Accès superutilisateur PostgreSQL ; externaliser les ancres de hash |
| T9 | Rejeu / altération d’un document | Modification du volume | Décision sur pièce falsifiée | Chiffrement authentifié GCM + SHA-256 au repos | Compromission simultanée clé + volume |
| T10 | Fuite par logs / erreurs | Stack traces, données personnelles | Divulgation | Gestionnaire d’erreurs générique, logs sans contenu métier, `include-message: never` | Logs applicatifs à auditer |
| T11 | Déni de service | Uploads massifs, requêtes coûteuses | Indisponibilité | Limites de taille, pagination bornée, pool DB | Absence de rate limiting global ; ajouter un reverse proxy limitant |
| T12 | Compromission de la chaîne CI/CD | Dépendance ou image malveillante | Backdoor | Versions épinglées, images officielles, build multi-étapes | Scan d’images et SBOM à ajouter |
| T13 | Mauvaise configuration | Secrets par défaut, DB exposée | Compromission totale | `.env` non versionné, secrets obligatoires, rôles DB séparés, ports DB non publiés | Revue de configuration et secrets manager à généraliser |
| T14 | Perte de données | Panne disque, suppression | Perte de dossiers/preuves | Volumes Docker, migrations versionnées | Sauvegardes chiffrées et PRA à définir |

**Hors périmètre actuel** : MFA, OCR/vérification de vivacité, contrôle d’intégrité matérielle des
pièces, signature électronique, détection de fraude inter-agences, rate limiting applicatif global.

## Protection des données et conformité

- **Minimisation** : seules les données nécessaires au KYC et au scoring sont collectées
  (identité, pièce, coordonnées, situation financière, historique de remboursement).
- **Finalité** : vérification d’identité, évaluation du risque de crédit, lutte contre la fraude
  et obligations de traçabilité.
- **Accès** : moindre privilège, cloisonnement par agence, admin sans accès aux dossiers,
  journalisation des consultations de documents.
- **Conservation** : à définir avec le service juridique et les exigences de la banque centrale.
  Proposition : dossiers et pièces conservés pendant la durée de la relation contractuelle
  augmentée du délai légal de conservation comptable, puis purge ou anonymisation ; audit
  conservé plus longtemps pour répondre aux obligations de preuve.
- **Droits des personnes** : procédure d’accès, de rectification et d’effacement à cadrer avec le
  DPO ; l’effacement doit composer avec les obligations légales de conservation.
- **Sous-traitants** : hébergement, sauvegarde et messagerie à encadrer par des contrats de
  sous-traitance et des clauses de localisation.
- **Cadre juridique** : la référence exacte à la loi togolaise relative à la protection des
  données à caractère personnel, ainsi qu’aux textes BCEAO/UEMOA applicables aux systèmes de
  paiement et à la lutte contre le blanchiment, **doit être validée par le service juridique**
  avant mise en production. Cette section est un emplacement réservé, pas un avis juridique.
- **Notification d’incident** : procédure à définir (détection, qualification, notification à
  l’autorité et aux personnes concernées, retour d’expérience).

## Tests

### Backend

```bash
cd backend
./mvnw test
```

- JUnit 5, Mockito (tests unitaires de service), Spring Security Test (RBAC et méthode).
- 29 tests : tests unitaires (masquage, canonicalisation, chiffrement, scoring, cloisonnement par
  agence) et tests d’intégration sur **PostgreSQL réel** via **Testcontainers**.
- Si le démon Docker n’est pas accessible, un **PostgreSQL embarqué** (Zonky) prend le relais avec
  les mêmes migrations ; le test reste donc exécutable sur un poste sans Docker.
- Couverture fonctionnelle : authentification et rotation, verrouillage, RBAC, workflow complet,
  quatre yeux, upload/chiffrement/altération, scoring, audit et détection de falsification.

### Frontend

```bash
cd frontend
npm test -- --watch=false
npm run build
```

Le build de production applique les budgets Angular. Les routes sont chargées paresseusement :
le bundle initial est de 422.79 kB brut (107.39 kB transféré), sous le seuil d’avertissement de
500 kB (voir `docs/DECISIONS.md`).

## Structure du dépôt

```
.
├── backend/
│   ├── src/main/java/com/sfd/kyc/
│   │   ├── auth/            # connexion, refresh, utilisateurs, agences
│   │   ├── kyc/             # dossiers, documents, scoring
│   │   ├── admin/           # utilisateurs et critères
│   │   └── common/          # sécurité, audit, erreurs, configuration
│   ├── src/main/resources/db/migration/   # V0 → V3
│   └── Dockerfile
├── frontend/
│   ├── src/app/core/        # services, modèles, guards, intercepteur
│   ├── src/app/features/    # login, dossiers, audit, admin
│   ├── src/app/shared/      # dialogues
│   ├── nginx.conf
│   └── Dockerfile
├── docker/postgres/init/    # rôles PostgreSQL
├── docs/DECISIONS.md
├── docker-compose.yml
├── .env.example
└── README.md
```

## Limites connues et évolutions

- **MFA** (TOTP ou WebAuthn) pour les rôles sensibles, en priorité superviseur et admin.
- **OCR** et contrôle de cohérence des pièces, détection de falsification, vérification de
  vivacité pour une capture mobile.
- **Antivirus** (ClamAV) sur les uploads et analyse des PDF.
- **Rate limiting** global et par utilisateur au niveau du reverse proxy.
- **Rotation des clés** de chiffrement et gestion par KMS/HSM ; ré-chiffrement progressif.
- **Sauvegardes chiffrées**, PITR PostgreSQL et exercices de restauration.
- **Observabilité** : métriques, traces, alertes sur les refus d’accès et les échecs de
  vérification d’audit.
- **Externalisation des ancres d’audit** (horodatage qualifié ou dépôt chez un tiers) pour
  renforcer la preuve.
- **Préchargement sélectif** des routes Angular et suivi du budget de bundle au fil des ajouts.
- **Accessibilité** : audit RGAA/WCAG complet et tests avec lecteur d’écran.

## Dépannage

### `docker compose up` échoue avec « Définir … dans .env »

Copier `.env.example` vers `.env` et renseigner toutes les variables marquées obligatoires.

### Le backend redémarre en boucle au premier lancement

La base n’est peut-être pas prête ou les rôles n’ont pas été créés. Vérifier :

```bash
docker compose logs db
docker compose logs backend
```

Le script d’initialisation ne s’exécute qu’avec un volume vide. Pour repartir de zéro :

```bash
docker compose down -v
docker compose up --build
```

### Mot de passe PostgreSQL modifié mais la connexion échoue

Les rôles sont créés au premier démarrage. Se connecter au conteneur et mettre à jour :

```bash
docker compose exec db psql -U postgres -d kyc \
  -c "ALTER ROLE kyc WITH PASSWORD 'nouveau';" \
  -c "ALTER ROLE kyc_app WITH PASSWORD 'nouveau';"
```

Puis mettre à jour `.env` et recréer le service backend.

### `KYC_ENCRYPTION_KEY doit être une clé AES-256`

La valeur doit être du base64 décodant **exactement 32 octets** :

```bash
openssl rand -base64 32
```

### Erreur 401 en boucle sur le frontend

Le refresh token a expiré ou a été révoqué (réutilisation détectée). Se reconnecter. Vérifier
l’horloge système : les jetons JWT sont sensibles au décalage horaire.

### Les tests d’intégration échouent faute de Docker

Ils basculent automatiquement sur PostgreSQL embarqué. Si ce repli échoue aussi, vérifier que le
répertoire temporaire est accessible en écriture et que le binaire PostgreSQL embarqué est
compatible avec la plateforme.

---

**Licence / usage** : projet interne SFD. Toute réutilisation doit faire l’objet d’une revue de
sécurité et d’une validation juridique.
