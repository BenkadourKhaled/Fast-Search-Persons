# FastSearch Persons

Backend de recherche de personnes **on-premise**, en Java 17 / Spring Boot 3.3 /
Lucene 9.12. Le moteur ne rend jamais une « identification » : il classe des
**candidats** avec un score, une confiance et une explication par champ.

> Toutes les données livrées avec le projet sont **synthétiques**. Aucun nom,
> aucune date et aucun identifiant ne provient d'une personne réelle.

> Version **0.1.0** — première version publique. Voir le
> [journal des modifications](CHANGELOG.md).

> Présentation GitHub complète : [français](README_GITHUB.md) · [English](README_GITHUB_EN.md)

## Ressources

- [README_GITHUB.md](README_GITHUB.md) — présentation complète pour GitHub en
  français : fonctionnalités, architecture, API, sécurité, données et démarrage
  rapide.
- [README_GITHUB_EN.md](README_GITHUB_EN.md) — la même présentation GitHub en
  anglais.
- [db/person_source_data.sql](db/person_source_data.sql) — export SQL des
  **196 personnes synthétiques** de `public.person_source` (UTF-8, 196 INSERT).
- [CHANGELOG.md](CHANGELOG.md) — historique des versions.
- [LICENSE](LICENSE) — licence MIT du projet.

## Principes

- **La base source est la source de vérité.** L'index Lucene est un artefact
  reconstructible, synchronisé par JDBC.
- **Résultats explicables.** Chaque candidat expose son score, sa confiance
  (`FORT`, `MOYEN`, `FAIBLE`) et le détail des comparaisons (nom, prénom, date,
  lieu, identifiant).
- **Rapprochement arabe/latin.** Normalisation arabe, translittérations,
  dictionnaire d'équivalences, inversion nom/prénom, particules, deux clés
  phonétiques et tolérance OCR/frappe.
- **Cloisonnement par groupes.** Les autorités `GROUP_xxx` viennent de
  l'authentification, jamais du corps de la requête.
- **Audit sans données personnelles.** Le journal stocke une empreinte SHA-256
  de la requête, jamais son texte brut.

## Pile et organisation

| Élément | Version / rôle |
|---|---|
| Java | 17 |
| Spring Boot | 3.3.13 |
| Lucene | 9.12.2 (index embarqué) |
| Base | H2 par défaut, PostgreSQL en profil `postgres` |
| Tests | JUnit 5, Spring Boot Test, ArchUnit |
| Build | Maven, un seul `pom.xml` |

```
src/main/java/com/fastsearch/persons/
├── analysis/   analyseurs Lucene (arabe, trigrammes, particules)
├── names/      normalisation, dictionnaire, phonétique, variantes
├── dates/      analyse et rapprochement de dates (Clock injectée)
├── search/     index, requêtes, scoring, explications
├── sync/       lecture JDBC par lots, validation SQL, sink Lucene
├── eval/       jeu synthétique, métriques, baselines
├── api/        API REST v1, DTO, gestion d'erreurs
├── config/     câblage Spring et données de démonstration
├── security/   HTTP Basic, rôles et groupes
└── audit/      journal JPA avec empreinte de requête
```

Les packages `analysis`, `names`, `dates` et `search` n'importent ni Spring ni
JPA et ne dépendent pas des couches externes. Un test ArchUnit le vérifie.

## Démarrage local

Prérequis : JDK 17, Maven 3.9+.

```bash
mvn clean test
mvn package
java -jar target/fastsearch-persons.jar
```

Par défaut :

- base H2 fichier dans `./data/fastsearch` ;
- index Lucene dans `./data/index` ;
- données de démonstration synthétiques activées (196 personnes) ;
- API sur `http://localhost:8080`.

Vérification :

```bash
curl http://localhost:8080/actuator/health
```

Pour repartir d'une base vide, arrêter l'application et supprimer `./data`.

### Profil PostgreSQL

```bash
export SPRING_PROFILES_ACTIVE=postgres
export FASTSEARCH_DB_URL=jdbc:postgresql://localhost:5432/fastsearch
export FASTSEARCH_DB_USER=fastsearch
export FASTSEARCH_DB_PASSWORD=change-me
java -jar target/fastsearch-persons.jar
```

Le profil `postgres` active par défaut les données de démonstration
(196 personnes) et le stock d'identités JDBC (`fastsearch_users`,
`fastsearch_authorities`). Pour les désactiver :

```bash
export FASTSEARCH_DEMO_ENABLED=false
```

## Configuration

Les valeurs sont serveur, jamais fournies par le client.

| Propriété | Défaut | Rôle |
|---|---|---|
| `fastsearch.index.path` | `./data/index` | Répertoire de l'index Lucene |
| `fastsearch.dictionary-path` | vide | CSV externe ; vide = dictionnaire embarqué |
| `fastsearch.search.candidate-limit` | `500` | Candidats Lucene rerankés au maximum |
| `fastsearch.search.max-page-size` | `100` | Taille de page maximale |
| `fastsearch.search.enable-coarse` | `true` | Active la clé phonétique grossière |
| `fastsearch.sync.batch-size` | `500` | Taille de lot JDBC par défaut |
| `fastsearch.sync.*-column` | colonnes standard | Colonnes de la table source |
| `fastsearch.eval.seed` | `42` | Graine du jeu synthétique |
| `fastsearch.eval.count` | `40` | Base de personnes synthétiques (12 à 120) |
| `fastsearch.eval.open-search-url` | vide | Baseline OpenSearch optionnelle |
| `fastsearch.demo.enabled` | `true` | Données synthétiques au démarrage |
| `fastsearch.security.default-users` | `false` | Comptes de démonstration codés en dur (désactivés) |
| `fastsearch.security.users` | comptes de test | Utilisateurs explicites (production) |

## Sécurité

HTTP Basic, sessions désactivées. Rôles : `ADMIN`, `ANALYST`, `READER`.
Groupes : autorités `GROUP_xxx`.

| Utilisateur | Mot de passe | Rôles | Groupes |
|---|---|---|---|
| `benkkadour.khaled@gmail.com` | `KHALED123` | ADMIN | A, B |
| `amina.benali@fastsearch.test` | `Amina-Analyste-2026` | ANALYST | A |
| `yassine.merabet@fastsearch.test` | `Yassine-Analyste-2026` | ANALYST | B |
| `sara.haddad@fastsearch.test` | `Sara-Lecteur-2026` | READER | A |
| `karim.bouzid@fastsearch.test` | `Karim-Lecteur-2026` | READER | B |

> **Comptes de test uniquement.** Les identifiants sont publics et stockés en
> clair dans `application.yml`. Les remplacer avant tout déploiement, par
> exemple avec `fastsearch.security.users` ou `SPRING_APPLICATION_JSON`.
> Les adresses `@fastsearch.test` sont fictives ; l'adresse administrateur est
> celle fournie pour la démonstration.

| Méthode | Chemin | Accès |
|---|---|---|
| GET | `/actuator/health`, `/actuator/info` | public |
| GET | `/actuator/**` (autres) | ADMIN |
| GET | `/api/v1/auth/me` | authentifié |
| GET, POST | `/api/v1/persons/search` | ADMIN, ANALYST, READER |
| GET | `/api/v1/admin/index/stats` | ADMIN |
| POST | `/api/v1/admin/index/reindex` | ADMIN |
| POST | `/api/v1/admin/index/sync` | ADMIN |
| GET | `/api/v1/admin/audit` | ADMIN |
| POST | `/api/v1/eval/run` | ADMIN, ANALYST |

## API de recherche

```bash
curl -u sara.haddad@fastsearch.test:Sara-Lecteur-2026 \
  --get --data-urlencode 'q=Walid Benali' \
  http://localhost:8080/api/v1/persons/search
```

Exemple arabe :

```bash
curl -u sara.haddad@fastsearch.test:Sara-Lecteur-2026 \
  --get --data-urlencode 'q=وليد بن علي' \
  http://localhost:8080/api/v1/persons/search
```

Recherche structurée :

```bash
curl -u sara.haddad@fastsearch.test:Sara-Lecteur-2026 \
  -H 'Content-Type: application/json' \
  -d '{
        "lastName": "Benali",
        "firstName": "Walid",
        "birthDate": "07/04/1992",
        "birthPlace": "Alger",
        "page": 0,
        "size": 20,
        "includeExplanation": true
      }' \
  http://localhost:8080/api/v1/persons/search
```

Réponse (extrait) :

```json
{
  "hits": [
    {
      "id": "D001",
      "lastName": "Benali",
      "firstName": "Walid",
      "fullName": "Benali Walid",
      "birthDate": "1992-04-07",
      "birthPrecision": "JOUR",
      "birthPlace": "Alger",
      "fatherName": "",
      "motherName": "",
      "ownerGroup": "A",
      "lang": "fr",
      "score": 1.0,
      "confidence": "FORT",
      "explanations": [
        {"field": "nom", "level": "EXACT", "detail": "comparaison directe : exact", "score": 1.0}
      ]
    }
  ],
  "total": 1,
  "page": 0,
  "size": 20,
  "reliable": true,
  "message": "…",
  "warnings": []
}
```

Champs de requête : `q`, `lastName`, `firstName`, `birthDate`, `birthPlace`,
`page`, `size`, `includeExplanation`. Le client **ne peut pas** choisir ses
groupes : ils sont déduits de son authentification.

### Confiance

- `FORT` : nom fort (exact, dictionnaire ou phonétique fine non grossière) et
  aucun indice contradictoire ; avec une date, celle-ci doit être compatible.
- `MOYEN` : rapprochement partiel, phonétique grossière, date seule ou
  corroboration insuffisante.
- `FAIBLE` : similarité faible ou aucun nom exploitable.

Une correspondance obtenue uniquement par la clé phonétique **grossière** ne
dépasse jamais `MOYEN`. Une entrée de dictionnaire est une équivalence
curatorée : elle peut atteindre `FORT`. Un nom de famille réduit à une particule
(`Ould`) avec une requête à un seul jeton est plafonné à `MOYEN`.

## Synchronisation JDBC

La table source standard contient :

```
id, last_name, first_name, birth_date, birth_precision, birth_flags,
birth_place, father_name, mother_name, owner_group, lang, updated_at, deleted
```

- Lecture par `PreparedStatement`, `setFetchSize`, commit après chaque lot.
- Les noms de table et de colonnes sont validés par expression régulière puis
  entourés de guillemets ; aucune valeur n'est concaténée dans le SQL.
- `updated_at` permet le mode incrémental ; `deleted` permet la suppression
  logique.
- La base source reste la source de vérité : l'index peut être reconstruit.

```bash
# Reconstruction complète
curl -u benkkadour.khaled@gmail.com:KHALED123 -X POST \
  'http://localhost:8080/api/v1/admin/index/reindex'

# Incrémental depuis une date ISO-8601
curl -u benkkadour.khaled@gmail.com:KHALED123 -X POST \
  'http://localhost:8080/api/v1/admin/index/sync?full=false&since=2026-01-01T00:00:00Z&batchSize=500'

# Statistiques d'index
curl -u benkkadour.khaled@gmail.com:KHALED123 \
  'http://localhost:8080/api/v1/admin/index/stats'

# Journal d'audit
curl -u benkkadour.khaled@gmail.com:KHALED123 \
  'http://localhost:8080/api/v1/admin/audit?page=0&size=50'
```

## Évaluation synthétique

```bash
curl -u amina.benali@fastsearch.test:Amina-Analyste-2026 -X POST \
  'http://localhost:8080/api/v1/eval/run?seed=42&count=24'
```

L'évaluation construit un **index temporaire**, jamais l'index de production,
et calcule :

- Recall@10, MRR, nDCG@10, précision@1 ;
- taux de faux positifs `FORT` ;
- métriques par catégorie : exact, inversion, arabe, translittération, faute,
  date, homonymes ;
- baseline SQL `LIKE` simulée ;
- baseline OpenSearch optionnelle (indiquée indisponible si non configurée,
  sans fabriquer de métriques) ;
- suggestions de lignes de dictionnaire.

Le jeu est déterministe pour une graine donnée. `count=12` produit 15 personnes
et 75 cas ; `count=24` produit 27 personnes et 147 cas.

## Docker

```bash
docker compose up --build
```

La composition démarre PostgreSQL 16 et l'application avec le profil `postgres`.
L'index est persisté dans le volume `indexdata`, la base dans `pgdata`.

```bash
# Arrêt sans perte de volumes
docker compose down

# Arrêt avec suppression des volumes (perte des données)
docker compose down -v
```

Pour désactiver les données de démonstration :

```bash
FASTSEARCH_DEMO_ENABLED=false docker compose up --build
```

## Tests

```bash
mvn test
```

La suite couvre la normalisation et le dictionnaire, les clés phonétiques, les
dates, le scoring, la synchronisation JDBC, l'évaluation, la sécurité, l'audit,
l'API et les règles ArchUnit. État vérifié : **62 tests, 0 échec**.

## Limites

Voir [LIMITS.md](LIMITS.md). En résumé : le dictionnaire et les règles
phonétiques sont des heuristiques à curer, les homonymes exigent des
corroborations, HTTP Basic doit être placé derrière TLS, et les comptes de
démonstration doivent être remplacés.

## Licence

Ce projet est publié sous licence **MIT**. Voir [LICENSE](LICENSE).

Copyright (c) 2026 FastSearch Persons contributors.

La licence MIT autorise l'utilisation, la modification, la distribution et la
sous-licence du code, y compris à des fins commerciales, sous réserve de
conserver l'avis de copyright et la clause de non-garantie.

> Les données synthétiques et la documentation sont fournies à titre
> d'exemple. Avant tout déploiement traitant des données réelles, vérifier
> les obligations légales applicables.
