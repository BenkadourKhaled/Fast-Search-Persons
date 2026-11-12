# Journal des modifications

Toutes les évolutions notables de **FastSearch Persons** sont documentées dans
ce fichier. Le format suit les principes de [Keep a Changelog](https://keepachangelog.com/fr/1.1.0/)
et le projet utilise le [versionnage sémantique](https://semver.org/lang/fr/).

## [0.1.0] — 2026-11-12

Première version publique.

### Ajouté

- API REST v1 pour la recherche en texte libre et structurée.
- Recherche multilingue arabe/latin avec normalisation, translittérations,
  inversion nom/prénom, particules et clés phonétiques.
- Scoring explicable par champ, niveaux de confiance et classement de candidats.
- Gestion des dates complètes, partielles et approximatives.
- Cloisonnement des résultats par groupes issus de l'authentification.
- Synchronisation JDBC vers un index Lucene reconstructible.
- Administration de l'index : statistiques, réindexation et synchronisation
  incrémentale.
- Journal d'audit avec empreinte SHA-256 des requêtes.
- Évaluation synthétique avec métriques et baselines.
- Interface React/TypeScript avec recherche FR/AR, affichage RTL et explications
  détaillées.
- Documentation française et anglaise, limites connues et licence MIT.

### Sécurité

- Authentification HTTP Basic, rôles `ADMIN`, `ANALYST` et `READER`.
- Comptes et données de démonstration explicitement synthétiques.
- Recommandations de durcissement documentées avant tout déploiement réel.

### Limites

- Les heuristiques de noms, de dates et de confiance doivent être évaluées sur
  un corpus métier avant une utilisation en production.
- Les comptes de démonstration doivent être remplacés.
- HTTP Basic doit être placé derrière TLS.
