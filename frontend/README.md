# FastSearch Persons — Frontend

Interface React 18 / TypeScript / Vite / Tailwind CSS du monorepo
**FastSearch Persons**. Elle est bilingue **français / arabe** avec bascule RTL
automatique et consomme uniquement l'API réelle : **aucun mock, aucun mode
démo**.

> Toutes les données affichées par le projet de démonstration sont
> **synthétiques**. Aucune personne réelle n'est représentée.

## Documentation

Toute la documentation de référence est centralisée à la racine du monorepo :

- [Guide frontend](../docs/frontend.md)
- [English frontend guide](../docs/frontend-en.md)
- [Architecture](../docs/architecture.md)
- [Référence API](../docs/api.md)
- [Déploiement](../docs/deployment.md)
- [README principal](../README.md)
- [Contribution](../CONTRIBUTING.md) · [Sécurité](../SECURITY.md) ·
  [Journal des modifications](../CHANGELOG.md)

## Démarrage rapide

```bash
cd frontend
npm ci
npm run dev
```

L'interface est disponible sur <http://localhost:5173>. Le proxy Vite cible
`http://localhost:8080` par défaut ; définir `FASTSEARCH_BACKEND_URL` pour
changer de backend. Voir [`.env.example`](.env.example).

## Vérifications

```bash
cd frontend
npm run typecheck
npm run lint
npm test
npm run build
```

## Licence

Projet publié sous licence **MIT**. Voir [LICENSE](../LICENSE).
