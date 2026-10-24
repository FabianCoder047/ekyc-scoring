# Frontend e-KYC

Application Angular standalone (Signals, formulaires réactifs, Angular Material) pour l’API
e-KYC & Scoring. La documentation complète, les comptes de démonstration et le modèle de menace
se trouvent dans le [`README.md`](../README.md) à la racine du dépôt.

## Développement

```bash
npm ci
npm start
```

L’application écoute sur <http://localhost:4200>. Le proxy de développement (`proxy.conf.json`)
transmet les requêtes `/api` vers <http://localhost:8080>, en conservant le préfixe imposé par le
`context-path` Spring Boot.

## Tests et build

```bash
npm test -- --watch=false
npm run build
```

Le build de production est écrit dans `dist/frontend/browser` et servi par nginx dans l’image
Docker. Les routes sont chargées paresseusement ; le bundle initial reste sous le budget de
500 kB défini dans `angular.json`.

## Organisation

- `src/app/core/` : services HTTP, authentification, guards, intercepteur, modèles et libellés ;
- `src/app/features/` : écrans agent, analyste, superviseur, audit et administration ;
- `src/app/shared/` : dialogues réutilisables ;
- `src/styles.scss` : thème et styles globaux.
