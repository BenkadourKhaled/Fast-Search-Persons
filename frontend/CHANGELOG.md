# Journal des modifications

Toutes les évolutions notables du frontend **FastSearch Persons** sont
documentées dans ce fichier. Le format suit les principes de
[Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet utilise le
[versionnage sémantique](https://semver.org/lang/fr/).

## [0.1.0] — 2026-11-12

Première version publique.

### Ajouté

- Interface React 18 / TypeScript / Vite / Tailwind CSS.
- Recherche structurée et recherche en texte libre.
- Affichage bilingue français/arabe avec mise en page RTL.
- Sélecteur de langue persistant dans l'en-tête, la connexion et les paramètres.
- Résultats avec score, niveau de confiance et explications détaillées.
- Fiche candidat, pagination serveur numérotée avec choix de la taille de page
  et états de chargement/erreur.
- Administration de l'index, journal d'audit et panneau d'évaluation.
- Client API REST réel, sans données simulées côté frontend.
- Tests unitaires Vitest sur l'i18n, l'affichage et les niveaux de confiance.
- Intégration continue GitHub Actions et Dependabot.
- Image Docker Nginx avec proxy same-origin et en-têtes de sécurité.
- Documentation française et anglaise et licence MIT.

### Sécurité

- Authentification HTTP Basic côté backend.
- Aucune donnée personnelle réelle dans le jeu de démonstration.
- Les résultats restent des candidats soumis à une décision humaine.
