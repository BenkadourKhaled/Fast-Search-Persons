# Journal des modifications

Toutes les évolutions notables de **FastSearch Persons** sont documentées dans
ce fichier. Le format suit les principes de
[Keep a Changelog](https://keepachangelog.com/fr/1.1.0/) et le projet utilise le
[versionnage sémantique](https://semver.org/lang/fr/).

## [Non publié]

### À venir

- Automatisation des parcours navigateur FR/AR/RTL dans la CI.
- Migrations versionnées (Flyway ou Liquibase) pour la production.
- Limitation de débit et reverse proxy TLS de référence.
- Politique de conservation et de purge du journal d'audit.

## [0.1.0] — 2026-11-15

Première version publique du **monorepo** FastSearch Persons, regroupant le
backend et le frontend dans un dépôt unique avec une documentation centralisée.

### Ajouté

- Monorepo `Fast-Search-Persons/` avec `backend/`, `frontend/`, `docs/` et
  fichiers communautaires à la racine.
- Import des historiques Git du backend et du frontend avec `git subtree`.
- Documentation centralisée : architecture, backend, API, frontend, déploiement,
  limites et index de lecture.
- Captures d'écran FR/AR/RTL dans `docs/screenshots/`.
- `docker-compose.yml` racine orchestrant PostgreSQL 16, le backend et le
  frontend Nginx, avec `.env.example` pour la configuration locale.
- Workflow CI monorepo pour le backend, le frontend, la vérification des liens
  Markdown et la validation du fichier Compose.
- Dependabot pour Maven, npm, GitHub Actions et Docker.
- Templates d'issue, template de pull request et fichier `.gitattributes`.
- Script `scripts/check-markdown-links.mjs` pour vérifier les liens relatifs.
- Favicon et métadonnées de l'interface.

### Backend

- API REST v1 pour la recherche en texte libre et structurée.
- Recherche multilingue arabe/latin avec normalisation, translittérations,
  inversion nom/prénom, particules et clés phonétiques.
- Scoring explicable par champ, niveaux de confiance et classement de candidats.
- Pagination serveur stable : `total` borné aux candidats rerankés et pages hors
  limites ramenées à la dernière page.
- Gestion des dates complètes, partielles et approximatives.
- Cloisonnement des résultats par groupes issus de l'authentification.
- Synchronisation JDBC vers un index Lucene reconstructible.
- Administration de l'index : statistiques, réindexation et synchronisation
  incrémentale.
- Journal d'audit avec empreinte SHA-256 des requêtes.
- Évaluation synthétique avec métriques et baselines.
- Documentation OpenAPI 3 / Swagger UI.
- Image Docker multi-étapes et healthcheck.

### Frontend

- Interface React 18 / TypeScript / Vite / Tailwind CSS.
- Recherche structurée et recherche en texte libre.
- Affichage bilingue français/arabe avec mise en page RTL.
- Sélecteur de langue persistant dans l'en-tête, la connexion et les paramètres.
- Résultats avec score, niveau de confiance et explications détaillées.
- Pagination serveur numérotée avec choix de la taille de page (10, 20, 50, 100).
- Fiche candidat, filtre de confiance et export JSON.
- Administration de l'index, journal d'audit et panneau d'évaluation.
- Client API REST réel, sans données simulées côté frontend.
- Tests unitaires Vitest sur l'i18n, l'affichage, les niveaux de confiance et la
  pagination.
- Image Docker Nginx avec proxy same-origin et en-têtes de sécurité.

### Modifié

- Les `README.md` des sous-projets deviennent des pointeurs vers la
  documentation centralisée.
- Les tags de version des sous-projets sont remplacés par un tag unique
  `v0.1.0` à la racine du monorepo.
- La CI et Dependabot sont consolidés à la racine.

### Corrigé

- `total` ne compte plus les candidats Lucene non rerankés.
- Une page hors limites n'expose plus une page vide.
- Le rang global reste correct après changement de page.
- Le filtre de confiance est réinitialisé à chaque nouvelle recherche.

### Sécurité

- Authentification HTTP Basic, sessions désactivées, mots de passe BCrypt.
- Groupes de lecture issus de l'authentification, jamais du client.
- Audit minimisé : empreinte SHA-256, pas de requête brute.
- Identifiants Basic du frontend conservés dans `sessionStorage` uniquement.
- En-têtes de sécurité Nginx (CSP, nosniff, frame-options, referrer-policy,
  permissions-policy).
- Toutes les données de démonstration sont synthétiques.

### Documentation

- README racine bilingue FR/EN avec badges, captures et démarrage rapide.
- Guide d'architecture avec diagrammes Mermaid.
- Guides backend et frontend, référence API et guide de déploiement.
- Politique de sécurité, guide de contribution et code de conduite.
- Limites connues mises à jour pour le périmètre du monorepo.

### Validation

- Backend : **65 tests, 0 échec**.
- Frontend : **25 tests, 0 échec**.
- Typecheck, lint et build frontend : OK.
- Parcours navigateur FR/AR/RTL : **14/14**.
- Pagination navigateur : **8/8**.
- Liens Markdown : OK.
