# Guide de contribution

Merci de contribuer à **FastSearch Persons**. Ce guide couvre le monorepo
complet : backend Spring Boot, frontend React et documentation centralisée.

En participant, vous acceptez le [Code de conduite](CODE_OF_CONDUCT.md). Les
vulnérabilités doivent être signalées selon la [politique de sécurité](SECURITY.md),
jamais dans une issue publique.

## 1. Structure du dépôt

```text
Fast-Search-Persons/
├── backend/     API Spring Boot, Lucene, audit, évaluation
├── frontend/    interface React, i18n FR/AR, pagination
├── docs/        documentation centralisée
├── scripts/     vérifications de documentation
└── .github/     CI, Dependabot, templates
```

Toute la documentation de référence vit dans `docs/` et à la racine. Les
`README.md` des sous-projets sont des pointeurs courts vers la documentation
centralisée.

## 2. Prérequis

- JDK **17** ;
- Maven **3.9+** ;
- Node.js **22** (ou 20.19+) et npm ;
- Docker et Docker Compose (optionnel).

## 3. Développement local

### Backend

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Le backend écoute sur `http://localhost:8080`. Les données synthétiques de
démonstration sont créées au premier démarrage.

### Frontend

```bash
cd frontend
npm ci
npm run dev
```

L'interface écoute sur `http://localhost:5173`. Si le port est occupé :

```bash
npm run dev -- --port 5174
```

Le proxy Vite cible `http://localhost:8080` par défaut. Définir
`FASTSEARCH_BACKEND_URL` pour changer de backend.

### Docker Compose

```bash
docker compose up --build
```

Interface : <http://localhost:8081> · API : <http://localhost:8080/api/v1>.

## 4. Vérifications avant une pull request

### Backend

```bash
cd backend
mvn verify
```

### Frontend

```bash
cd frontend
npm run typecheck
npm run lint
npm test
npm run build
```

### Documentation

```bash
node scripts/check-markdown-links.mjs
```

### Parcours fonctionnel

- Vérifier la connexion et la recherche en **français**.
- Vérifier la connexion et la recherche en **arabe**, avec RTL.
- Vérifier la pagination sur plusieurs tailles de page (10, 20, 50, 100).
- Vérifier la fiche candidat, l'administration, l'audit et l'évaluation selon
  le rôle.
- Vérifier que les données affichées restent synthétiques.

## 5. Conventions backend

- Java 17, quatre espaces, pas de wildcard imports.
- Les packages `analysis`, `names`, `dates` et `search` ne doivent pas dépendre
  de Spring, de JPA ou de la couche web. Un test ArchUnit le vérifie.
- Les messages destinés à l'utilisateur restent en français côté backend ; le
  frontend gère leur traduction à l'affichage.
- Toute nouvelle règle de scoring ou de confiance doit être couverte par des
  tests.
- La pagination doit rester bornée : `total` compte les candidats rerankés et
  une page hors limites est ramenée à la dernière page.
- Ne jamais concaténer une valeur utilisateur dans une requête SQL.
- Les identifiants de démonstration restent clairement marqués comme tels.

## 6. Conventions frontend

- TypeScript strict, composants fonctionnels.
- Utiliser les utilitaires Tailwind logiques (`ms`, `me`, `ps`, `pe`, `start`,
  `end`) pour rester compatible RTL.
- Toute nouvelle chaîne visible doit être ajoutée aux dictionnaires FR et AR
  dans `src/lib/i18n.ts`.
- Les appels réseau passent par `src/lib/api.ts`.
- Ne jamais ajouter de mock, de mode démo ou de données simulées dans le client.
- Les identifiants Basic restent dans `sessionStorage`, jamais dans
  `localStorage`.
- Les composants interactifs doivent rester accessibles (libellés ARIA,
  navigation clavier, focus visible).
- Les tests Vitest couvrent les utilitaires et les comportements critiques.

## 7. Conventions de documentation

- La documentation de référence est centralisée dans `docs/`.
- Le README racine est la vitrine ; les README de sous-projets sont des
  pointeurs.
- Les liens relatifs doivent être valides et vérifiés par
  `node scripts/check-markdown-links.mjs`.
- Les captures d'écran vont dans `docs/screenshots/` avec un texte alternatif
  descriptif.
- Mettre à jour le [journal des modifications](CHANGELOG.md) pour toute
  évolution notable.
- Ne jamais inclure de donnée personnelle réelle, de secret ou d'identifiant de
  production.

## 8. Commits et branches

Le projet suit les principes des **Conventional Commits** :

```text
feat: add numbered pagination to search results
fix: clamp out-of-range search pages
docs: centralize documentation in the monorepo
test: cover pagination boundaries
chore: update CI workflow
```

- Une branche par sujet : `feat/...`, `fix/...`, `docs/...`.
- Les commits restent ciblés et explicites.
- Ne pas mélanger refactoring, fonctionnalité et documentation dans un même
  commit lorsque c'est évitable.
- Ne jamais committer `target/`, `node_modules/`, `dist/`, `data/` ou un fichier
  `.env`.

## 9. Checklist de pull request

- [ ] Le changement est décrit et lié au besoin.
- [ ] Les tests backend passent (`mvn verify`).
- [ ] Les vérifications frontend passent (`typecheck`, `lint`, `test`, `build`).
- [ ] Les liens Markdown passent.
- [ ] Les écrans FR et AR ainsi que le RTL sont vérifiés.
- [ ] La pagination est vérifiée si les résultats changent.
- [ ] Aucun secret, `target/`, `node_modules/`, `dist/` ou donnée réelle n'est
      ajouté.
- [ ] Aucun mock ou mode démo n'est introduit.
- [ ] La documentation et le changelog sont à jour.
- [ ] Les risques et impacts (API, sécurité, configuration) sont précisés.

## 10. Questions

Pour une question fonctionnelle ou technique, ouvrez une issue en utilisant le
template approprié. Pour une vulnérabilité, utilisez le canal privé décrit dans
[SECURITY.md](SECURITY.md).
