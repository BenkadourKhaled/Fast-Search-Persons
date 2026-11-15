# FastSearch Persons — Frontend

Interface web de **FastSearch Persons**, un moteur on-premise de rapprochement
de personnes. L'application ne produit jamais une identification automatique :
elle affiche des **candidats** avec un score, un niveau de confiance et une
explication par champ.

**Version 0.1.0 — première version publique.** Voir le
[journal des modifications](CHANGELOG.md).

> Toutes les données affichées par le projet de démonstration sont
> **synthétiques**. Aucune personne réelle n'est représentée.

## Fonctionnalités

- Recherche structurée (nom, prénom, date, lieu) et recherche en texte libre.
- Interface bilingue **français / arabe**, avec bascule RTL automatique.
- Persistance de la langue choisie et prise en charge de `Noto Naskh Arabic`.
- Cartes de résultats avec score, confiance et explication par champ.
- Pagination serveur avec navigation numérotée et choix de la taille de page.
- Fiche détaillée d'un candidat.
- Administration de l'index : statistiques, réindexation et synchronisation.
- Journal d'audit et panneau d'évaluation synthétique.
- Authentification HTTP Basic et gestion des rôles côté backend.
- Client API réel : aucun mode démo ou données simulées côté frontend.

## Prérequis

- Node.js **20.19+** ou **22.12+**
- npm
- Backend FastSearch Persons démarré sur `http://localhost:8080`

## Démarrage

```bash
npm install
npm run dev
```

L'application est disponible sur `http://localhost:5173`.

Vite redirige `/api` et `/actuator` vers le backend. Pour utiliser un autre
serveur :

```bash
FASTSEARCH_BACKEND_URL=http://localhost:18080 npm run dev
```

Sous PowerShell :

```powershell
$env:FASTSEARCH_BACKEND_URL = "http://localhost:18080"
npm run dev
```

## Scripts

| Commande | Rôle |
|---|---|
| `npm run dev` | Serveur de développement Vite |
| `npm run build` | Build de production dans `dist/` |
| `npm run preview` | Prévisualisation du build |
| `npm run typecheck` | Vérification TypeScript |
| `npm run lint` | Analyse ESLint |
| `npm test` | Tests unitaires Vitest |
| `npm run test:watch` | Tests en mode watch |

## Qualité et CI

- Tests unitaires sur l'i18n, les libellés et les niveaux de confiance.
- Vérifications `typecheck`, `lint`, `test` et `build` dans GitHub Actions.
- Audit des dépendances de production : `npm audit --omit=dev`.
- Aucun mock ni mode démo dans le client API.

## Docker

Le frontend peut être servi par Nginx, avec proxy same-origin vers le backend :

```bash
docker build -t fastsearch-persons-frontend .
docker run --rm -p 8080:80 \
  -e BACKEND_UPSTREAM=http://host.docker.internal:8080 \
  fastsearch-persons-frontend
```

L'image finale contient uniquement le build statique et Nginx. Elle ajoute les
en-têtes de sécurité, la compression et le fallback SPA. Sous Linux, ajouter
`--add-host=host.docker.internal:host-gateway` si nécessaire.

## Configuration

Le backend est configurable depuis l'écran de connexion et le menu des
paramètres. La valeur vide utilise le proxy Vite de même origine.

La langue est enregistrée dans `localStorage` sous la clé
`fastsearch_language`. Le choix est appliqué à `document.documentElement.lang`
et `dir` (`ltr` ou `rtl`).

## Structure

```text
src/
├── components/       écrans et composants de l'interface
├── lib/              client API, authentification, i18n, affichage
├── App.tsx           application principale
├── main.tsx          point d'entrée React
└── types.ts          types partagés
```

## Documentation associée

- [README_EN.md](README_EN.md) — English version.
- [CHANGELOG.md](CHANGELOG.md) — historique des versions.
- [SECURITY.md](SECURITY.md) — politique de sécurité.
- [CONTRIBUTING.md](CONTRIBUTING.md) — guide de contribution.
- [LICENSE](LICENSE) — licence MIT.

## Licence

Projet distribué sous licence **MIT**. Voir [LICENSE](LICENSE).

Copyright (c) 2026 FastSearch Persons contributors.
