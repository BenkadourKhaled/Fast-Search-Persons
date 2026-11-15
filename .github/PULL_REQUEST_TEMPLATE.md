## Résumé

Décrire le changement et le besoin couvert.

## Type de changement

- [ ] Correction de bug
- [ ] Nouvelle fonctionnalité
- [ ] Documentation
- [ ] Refactoring
- [ ] CI / outillage
- [ ] Sécurité

## Vérifications

- [ ] `cd backend && mvn verify` passe.
- [ ] `cd frontend && npm run typecheck` passe.
- [ ] `cd frontend && npm run lint` passe.
- [ ] `cd frontend && npm test` passe.
- [ ] `cd frontend && npm run build` passe.
- [ ] `node scripts/check-markdown-links.mjs` passe.
- [ ] Les écrans FR et AR ainsi que le mode RTL sont vérifiés.
- [ ] La pagination est vérifiée si les résultats changent.
- [ ] Aucun secret, `target/`, `node_modules/`, `dist/` ou donnée réelle n'est ajouté.
- [ ] Aucun mock ou mode démo n'est introduit.
- [ ] La documentation et le changelog sont à jour.

## Impact et risques

Préciser les endpoints, configurations, migrations ou changements de sécurité.

## Captures

Ajouter des captures avant/après si l'interface change.
