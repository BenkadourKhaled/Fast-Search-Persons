# Politique de sécurité

## Versions supportées

| Version | Supportée |
|---|---|
| `0.1.x` | Oui |
| `< 0.1` | Non |

## Signaler une vulnérabilité

**N'ouvrez pas d'issue publique** pour une vulnérabilité de sécurité.

Utilisez le signalement privé de GitHub (**Security → Advisories → Report a
vulnerability**) pour ce dépôt. Si ce canal n'est pas disponible, contactez les
mainteneurs par un canal privé et incluez :

- la version ou le commit concerné ;
- le composant ou l'endpoint impacté (backend, frontend, Docker, documentation) ;
- les étapes de reproduction ou une preuve de concept ;
- l'impact potentiel ;
- toute mesure d'atténuation suggérée.

Nous accuserons réception dès que possible et coordonnerons un correctif ainsi
qu'un calendrier de divulgation avec vous. Ce projet est maintenu sur une base
bénévole : aucun délai de réponse contractuel n'est garanti.

## Périmètre

Sont concernés :

- l'API REST et le moteur de recherche backend ;
- l'interface React et son client API ;
- les images Docker et la configuration Nginx ;
- les workflows GitHub Actions ;
- la documentation et les scripts de vérification.

Ne sont **pas** des vulnérabilités :

- la présence des comptes de démonstration publics, documentés comme tels ;
- l'absence de MFA, de rate limiting ou de migrations versionnées, déjà
  documentée dans les [limites](docs/limitations.md) ;
- les faux positifs ou limites de qualité du rapprochement, qui relèvent des
  limites fonctionnelles ;
- les données synthétiques du jeu de démonstration.

## Socle de sécurité

- Toutes les personnes, tous les noms, dates, lieux et identifiants livrés sont
  **synthétiques**.
- Les comptes de démonstration sont publics et doivent être remplacés avant tout
  déploiement réel.
- HTTP Basic doit être servi derrière **TLS**.
- Les sessions serveur sont désactivées (`STATELESS`) ; les mots de passe sont
  hachés avec BCrypt.
- Les groupes de lecture viennent de l'authentification, jamais du corps de la
  requête.
- Le journal d'audit stocke une **empreinte SHA-256** de la requête, jamais son
  texte brut ni les valeurs des champs.
- Le frontend conserve les identifiants Basic dans `sessionStorage`, pour
  l'onglet courant uniquement, et les efface au changement d'URL de backend.
- Nginx ajoute une CSP, `X-Content-Type-Options`, `X-Frame-Options`,
  `Referrer-Policy` et `Permissions-Policy`.
- Le client API ne contient ni mock ni mode démo.
- Les résultats sont des **candidats**, jamais des identifications.

## Durcissement avant production

- [ ] Terminer TLS sur un reverse proxy de confiance.
- [ ] Remplacer les comptes de démonstration et utiliser un gestionnaire de
      secrets.
- [ ] Désactiver les données de démonstration
      (`FASTSEARCH_DEMO_ENABLED=false`).
- [ ] Restreindre les origines CORS au domaine réel.
- [ ] Ajouter une limitation de débit et une protection anti-abus.
- [ ] Remplacer `ddl-auto=update` par Flyway ou Liquibase.
- [ ] Définir une politique de conservation et de purge de l'audit.
- [ ] Protéger la base et les volumes de sauvegarde.
- [ ] Mettre en place une supervision des healthchecks et des logs.
- [ ] Effectuer une revue de sécurité et un test de charge sur un corpus
      représentatif.
- [ ] Vérifier les obligations légales applicables aux données réelles.

## Bonnes pratiques pour les contributeurs

- Ne jamais committer de secret, de mot de passe, de jeton ou de donnée
  personnelle réelle.
- Ne pas ajouter de données réelles dans les tests, les captures ou la
  documentation.
- Utiliser des données synthétiques et des identifiants fictifs.
- Signaler toute dépendance vulnérable via le canal privé, pas dans une issue
  publique.
- Maintenir les dépendances à jour via Dependabot et les revues de sécurité.

## Résumé en anglais

Please **do not open a public issue** for a security vulnerability. Use GitHub's
private vulnerability reporting (**Security → Advisories → Report a
vulnerability**) and include the affected version, component, reproduction
steps, impact and any suggested mitigation. All bundled data is synthetic; demo
accounts must be replaced before deployment; HTTP Basic must be served behind
TLS; the audit log stores a SHA-256 fingerprint, not the raw query. See
[Known limitations](docs/limitations.md) for documented, non-security
limitations.
