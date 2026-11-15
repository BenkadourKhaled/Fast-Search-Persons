# Contributing

Merci de contribuer à l'interface FastSearch Persons.

## Développement

Prérequis : Node.js 22 (ou 20.19+), npm, et un backend FastSearch Persons.

```bash
npm ci
npm run dev
```

Le proxy Vite utilise `http://localhost:8080` par défaut. Copier
`.env.example` vers `.env` ou définir `FASTSEARCH_BACKEND_URL` pour changer de
backend.

## Avant une pull request

1. Ajouter ou adapter les tests unitaires.
2. Exécuter :

```bash
npm run typecheck
npm run lint
npm test
npm run build
```

3. Vérifier les deux langues (FR et AR) et le sens RTL.
4. Ne pas ajouter de données simulées ou de mode démo.
5. Ne pas committer `node_modules/`, `dist/` ou un fichier `.env`.

## Style

- TypeScript strict, composants fonctionnels.
- Utiliser les utilitaires logiques Tailwind (`ms`, `me`, `ps`, `pe`, `start`,
  `end`) pour rester compatible RTL.
- Toute nouvelle chaîne visible doit être ajoutée aux dictionnaires FR et AR
  dans `src/lib/i18n.ts`.
- Les appels réseau passent par `src/lib/api.ts`.

## Sécurité

Les vulnérabilités doivent être signalées selon `SECURITY.md`, pas dans une
issue publique.
