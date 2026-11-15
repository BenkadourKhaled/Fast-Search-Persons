# FastSearch Persons

**Moteur on-premise de rapprochement de personnes, explicable et bilingue FR/AR.**

Version **0.1.0** · Licence MIT

FastSearch Persons recherche des **candidats**, jamais des identifications
automatiques. Chaque résultat est accompagné d'un score, d'un niveau de
confiance et d'une explication par champ. Toutes les données de démonstration
sont **synthétiques** : aucune personne réelle n'est représentée.

## Points clés

- Recherche multilingue arabe / latin : normalisation, translittérations,
  inversion nom/prénom, particules et clés phonétiques.
- Recherche structurée (nom, prénom, date, lieu) et texte libre.
- Scoring explicable, niveaux de confiance et classement stable des candidats.
- Pagination serveur avec navigation numérotée et taille de page configurable.
- Interface React bilingue **français / arabe** avec mise en page RTL.
- Cloisonnement des résultats par groupes issus de l'authentification.
- Journal d'audit avec empreinte SHA-256 des requêtes.
- Évaluation synthétique avec métriques et baselines.
- API REST documentée avec OpenAPI 3 / Swagger UI.
- Intégration continue GitHub Actions et Dependabot.

## Architecture

```text
Navigateur
   │
   ▼
frontend/  React 18 · TypeScript · Vite · Tailwind CSS
   │  REST /api/v1  (HTTP Basic)
   ▼
backend/   Spring Boot 3.3 · Java 17 · Lucene
   │
   ├── index Lucene reconstructible
   └── H2 (local) ou PostgreSQL (production)
```

## Structure du dépôt

```text
Fast-Search-Persons/
├── backend/    API Spring Boot, recherche Lucene, audit, évaluation
├── frontend/   interface React, i18n FR/AR, pagination, Docker Nginx
├── README.md
├── LICENSE
└── .gitignore
```

## Démarrage rapide

### Prérequis

- JDK **17** et Maven **3.9+** pour le backend.
- Node.js **22** (ou 20.19+) et npm pour le frontend.

### 1. Backend

```bash
cd backend
mvn spring-boot:run
```

Ou après construction :

```bash
mvn package
java -jar target/fastsearch-persons.jar
```

Le backend écoute sur `http://localhost:8080`. Les données synthétiques de
démonstration sont créées au premier démarrage.

### 2. Frontend

```bash
cd frontend
npm ci
npm run dev
```

L'interface est disponible sur `http://localhost:5173`. Si ce port est déjà
utilisé :

```bash
npm run dev -- --port 5174
```

Le proxy Vite redirige `/api` et `/actuator` vers `http://localhost:8080`.
Pour cibler un autre backend, définir `FASTSEARCH_BACKEND_URL` ou copier
`frontend/.env.example` vers `frontend/.env`.

### Compte de démonstration

```text
Identifiant : benkkadour.khaled@gmail.com
Mot de passe : KHALED123
Rôle         : ADMIN
```

> Ces identifiants sont **publics et réservés à la démonstration locale**. Ils
> doivent être remplacés avant tout déploiement réel.

## API et documentation

- Swagger UI : <http://localhost:8080/swagger-ui.html>
- Document OpenAPI : <http://localhost:8080/v3/api-docs>
- Santé : <http://localhost:8080/actuator/health>

Exemple de recherche paginée :

```bash
curl -u benkkadour.khaled@gmail.com:KHALED123 \
  'http://localhost:8080/api/v1/persons/search?q=Benali&page=0&size=10'
```

`page` est indexé à partir de 0. `size` est borné à 100. La réponse contient
`hits`, `total`, `page`, `size`, `reliable`, `message` et `warnings`.

## Tests et qualité

Backend :

```bash
cd backend
mvn verify
```

Frontend :

```bash
cd frontend
npm run typecheck
npm run lint
npm test
npm run build
```

État vérifié : **65 tests backend**, **25 tests frontend**, parcours navigateur
FR/AR/RTL validé, pagination testée en navigateur.

## Docker

Backend et PostgreSQL :

```bash
cd backend
docker compose up --build
```

Frontend servi par Nginx, avec proxy same-origin vers le backend :

```bash
cd frontend
docker build -t fastsearch-persons-frontend .
docker run --rm -p 8081:80 \
  -e BACKEND_UPSTREAM=http://host.docker.internal:8080 \
  fastsearch-persons-frontend
```

Sous Linux, ajouter `--add-host=host.docker.internal:host-gateway` si nécessaire.

## Sécurité et éthique

- Les résultats sont des **candidats**, jamais des identifications.
- Les données livrées sont synthétiques et les comptes de démonstration doivent
  être remplacés.
- HTTP Basic doit être servi derrière TLS en production.
- Le journal d'audit conserve une empreinte SHA-256, pas la requête brute.
- Les vulnérabilités doivent être signalées selon les politiques de sécurité
  des deux sous-projets, pas dans une issue publique.

## Documentation

### Backend

- [README backend](backend/README.md)
- [Guide GitHub](backend/README_GITHUB.md) · [English](backend/README_GITHUB_EN.md)
- [Limites connues](backend/LIMITS.md)
- [Sécurité](backend/SECURITY.md)
- [Contribution](backend/CONTRIBUTING.md)
- [Journal des modifications](backend/CHANGELOG.md)

### Frontend

- [README frontend](frontend/README.md) · [English](frontend/README_EN.md)
- [Sécurité](frontend/SECURITY.md)
- [Contribution](frontend/CONTRIBUTING.md)
- [Journal des modifications](frontend/CHANGELOG.md)

## Licence

Projet publié sous licence **MIT**. Voir [LICENSE](LICENSE).
