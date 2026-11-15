# Guide backend

Le backend **FastSearch Persons** est une application Java 17 / Spring Boot 3.3
/ Lucene 9.12. Il expose une API REST versionnée, applique le cloisonnement par
groupes, journalise les recherches sous forme d'empreinte et fournit un module
d'évaluation synthétique.

> Toutes les données livrées avec le projet sont **synthétiques**. Aucun nom,
> aucune date et aucun identifiant ne provient d'une personne réelle.

## 1. Stack technique

| Élément | Version / rôle |
|---|---|
| Java | 17 |
| Spring Boot | 3.3.13 |
| Spring MVC / Validation | API REST, DTO, erreurs structurées |
| Spring Security | HTTP Basic, rôles, groupes, BCrypt |
| Spring Data JPA / JDBC | Audit JPA, lecture source par lots |
| Apache Lucene | 9.12.2, index embarqué |
| Base | H2 par défaut, PostgreSQL en profil `postgres` |
| Tests | JUnit 5, Spring Boot Test, ArchUnit |
| Build | Maven, un seul `pom.xml` |
| Documentation API | springdoc-openapi / Swagger UI |

## 2. Organisation des packages

```text
backend/src/main/java/com/fastsearch/persons/
├── analysis/   analyseurs Lucene (arabe, trigrammes, particules)
├── names/      normalisation, dictionnaire, phonétique, variantes
├── dates/      analyse et rapprochement de dates (Clock injectée)
├── search/     index, requêtes, scoring, explications, pagination
├── sync/       lecture JDBC par lots, validation SQL, sink Lucene
├── eval/       jeu synthétique, métriques, baselines
├── api/        API REST v1, DTO, gestion d'erreurs
├── config/     câblage Spring et données de démonstration
├── security/   HTTP Basic, rôles et groupes
└── audit/      journal JPA avec empreinte de requête
```

Les packages `analysis`, `names`, `dates` et `search` n'importent ni Spring ni
JPA. Un test ArchUnit vérifie cette indépendance.

## 3. Démarrage local

### Prérequis

- JDK **17** ;
- Maven **3.9+**.

### Construction et tests

```bash
cd backend
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

Créer une base et un rôle (adapter selon l'installation) :

```bash
psql -U postgres -c "CREATE USER fastsearch WITH PASSWORD 'fastsearch';"
psql -U postgres -c "CREATE DATABASE fastsearch OWNER fastsearch;"
```

Lancer avec le profil `postgres` :

```bash
export SPRING_PROFILES_ACTIVE=postgres
export FASTSEARCH_DB_URL=jdbc:postgresql://localhost:5432/fastsearch
export FASTSEARCH_DB_USER=fastsearch
export FASTSEARCH_DB_PASSWORD=change-me
java -jar target/fastsearch-persons.jar
```

Sous PowerShell :

```powershell
$env:SPRING_PROFILES_ACTIVE = 'postgres'
$env:FASTSEARCH_DB_URL = 'jdbc:postgresql://localhost:5432/fastsearch'
$env:FASTSEARCH_DB_USER = 'fastsearch'
$env:FASTSEARCH_DB_PASSWORD = 'change-me'
java -jar target\fastsearch-persons.jar
```

Le profil `postgres` active les données de démonstration et le stock
d'identités JDBC (`fastsearch_users`, `fastsearch_authorities`). Pour désactiver
les données de démonstration :

```bash
export FASTSEARCH_DEMO_ENABLED=false
```

### Importer l'export SQL

Le fichier [`db/person_source_data.sql`](../backend/db/person_source_data.sql)
contient les **196 INSERT** de `public.person_source`, encadrés par une
transaction.

```bash
psql -U fastsearch -d fastsearch -f db/person_source_data.sql
```

Vérification :

```sql
SELECT COUNT(*) FROM public.person_source;
-- 196

SELECT lang, COUNT(*)
FROM public.person_source
GROUP BY lang
ORDER BY lang;
-- ar : 103
-- fr : 93
```

## 4. Configuration

Les valeurs sont serveur, jamais fournies par le client.

| Propriété | Défaut | Rôle |
|---|---|---|
| `server.port` | `8080` | Port HTTP |
| `fastsearch.index.path` | `./data/index` | Répertoire de l'index Lucene |
| `fastsearch.dictionary-path` | vide | CSV externe ; vide = dictionnaire embarqué |
| `fastsearch.search.candidate-limit` | `500` | Candidats Lucene rerankés au maximum |
| `fastsearch.search.max-page-size` | `100` | Taille de page maximale |
| `fastsearch.search.enable-coarse` | `true` | Active la clé phonétique grossière |
| `fastsearch.sync.batch-size` | `500` | Taille de lot JDBC par défaut |
| `fastsearch.sync.table` | `person_source` | Table source |
| `fastsearch.sync.*-column` | colonnes standard | Mapping des colonnes |
| `fastsearch.eval.seed` | `42` | Graine du jeu synthétique |
| `fastsearch.eval.count` | `40` | Nombre de cas par défaut (12 à 120) |
| `fastsearch.eval.open-search-url` | vide | Baseline OpenSearch optionnelle |
| `fastsearch.demo.enabled` | `true` | Données synthétiques au démarrage |
| `fastsearch.security.store` | `memory` | `memory` ou `jdbc` |
| `fastsearch.security.default-users` | `false` | Comptes de secours si aucun utilisateur |
| `fastsearch.security.users` | comptes de test | Utilisateurs explicites |
| `fastsearch.cors.allowed-origins` | origines Vite | Origines CORS autorisées |

### Variables d'environnement PostgreSQL

| Variable | Défaut |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `postgres` |
| `FASTSEARCH_DB_URL` | `jdbc:postgresql://localhost:5432/fastsearch` |
| `FASTSEARCH_DB_USER` | `fastsearch` |
| `FASTSEARCH_DB_PASSWORD` | `fastsearch` |
| `FASTSEARCH_INDEX_PATH` | `./data/index` |
| `FASTSEARCH_DEMO_ENABLED` | `true` |
| `FASTSEARCH_SECURITY_STORE` | `jdbc` |

## 5. Sécurité et comptes

HTTP Basic, sessions désactivées. Rôles : `ADMIN`, `ANALYST`, `READER`.
Groupes : autorités `GROUP_xxx`.

| Utilisateur | Mot de passe | Rôles | Groupes |
|---|---|---|---|
| `benkkadour.khaled@gmail.com` | `KHALED123` | ADMIN | A, B |
| `amina.benali@fastsearch.test` | `Amina-Analyste-2026` | ANALYST | A |
| `yassine.merabet@fastsearch.test` | `Yassine-Analyste-2026` | ANALYST | B |
| `sara.haddad@fastsearch.test` | `Sara-Lecteur-2026` | READER | A |
| `karim.bouzid@fastsearch.test` | `Karim-Lecteur-2026` | READER | B |

> [!WARNING]
> Ces comptes sont **publics et réservés à la démonstration locale**. Les
> identifiants sont stockés en clair dans `application.yml`. Les remplacer avant
> tout déploiement, par exemple avec `fastsearch.security.users` ou
> `SPRING_APPLICATION_JSON`. Les adresses `@fastsearch.test` sont fictives.

### Matrice d'accès

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

La référence complète des endpoints, paramètres et réponses se trouve dans
[la référence API](api.md).

### Documentation OpenAPI

- Swagger UI : <http://localhost:8080/swagger-ui.html>
- Document JSON : <http://localhost:8080/v3/api-docs>

Swagger UI permet de saisir les identifiants HTTP Basic. Les endpoints métier
restent protégés par les rôles.

## 6. Recherche

### Texte libre

```bash
curl -u sara.haddad@fastsearch.test:Sara-Lecteur-2026 \
  --get --data-urlencode 'q=Oualid Benali 07/04/1992' \
  http://localhost:8080/api/v1/persons/search
```

Exemple arabe :

```bash
curl -u sara.haddad@fastsearch.test:Sara-Lecteur-2026 \
  --get --data-urlencode 'q=وليد بن علي' \
  http://localhost:8080/api/v1/persons/search
```

### Recherche structurée

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

### Pagination

`page` est indexé à partir de **0** et `size` est borné par
`fastsearch.search.max-page-size` (100 par défaut). La réponse renvoie `total`,
`page` et `size` ; `total` compte les candidats effectivement rerankés, dans la
limite de `candidate-limit`. Une page hors limites est ramenée à la dernière
page disponible, ce qui évite les pages vides après un changement de corpus.

```bash
curl -u sara.haddad@fastsearch.test:Sara-Lecteur-2026 \
  'http://localhost:8080/api/v1/persons/search?q=Benali&page=0&size=10'
```

L'interface propose 10, 20, 50 ou 100 résultats par page et une navigation
numérotée avec les pages précédente/suivante.

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

## 7. Synchronisation JDBC

La table source standard contient :

```text
id, last_name, first_name, birth_date, birth_precision, birth_flags,
birth_place, father_name, mother_name, owner_group, lang, updated_at, deleted
```

Fonctionnement :

1. lecture de `person_source` via `PreparedStatement` ;
2. `setFetchSize` pour borner la mémoire côté pilote ;
3. traitement par lots (`fastsearch.sync.batch-size`, défaut `500`) ;
4. écriture des upserts et suppressions dans Lucene ;
5. `commit()` après chaque lot ;
6. rapport final : lignes lues, documents indexés, supprimés, lots, dernier
   identifiant, avertissements.

Sécurité SQL :

- les noms de table et de colonnes sont validés par expression régulière ;
- les identifiants sont entourés de guillemets ;
- les valeurs sont toujours liées par `PreparedStatement` ;
- aucune concaténation de valeur utilisateur dans le SQL.

### Modes

| Mode | Comportement |
|---|---|
| `FULL` | Vide l'index puis relit toute la source |
| `INCREMENTAL` | Lit les lignes dont `updated_at >= since` |
| `INCREMENTAL` sans `since` | Lit depuis l'origine et ajoute un avertissement |

Les lignes avec `deleted = true` sont supprimées de l'index.

### API de synchronisation

```bash
# Réindexation complète
curl -u benkkadour.khaled@gmail.com:KHALED123 -X POST \
  http://localhost:8080/api/v1/admin/index/reindex

# Incrémental depuis une date ISO-8601
curl -u benkkadour.khaled@gmail.com:KHALED123 -X POST \
  'http://localhost:8080/api/v1/admin/index/sync?full=false&since=2026-01-01T00:00:00Z&batchSize=500'

# Statistiques d'index
curl -u benkkadour.khaled@gmail.com:KHALED123 \
  http://localhost:8080/api/v1/admin/index/stats

# Journal d'audit
curl -u benkkadour.khaled@gmail.com:KHALED123 \
  'http://localhost:8080/api/v1/admin/audit?page=0&size=50'
```

## 8. Journal d'audit

Le journal stocke :

- l'utilisateur, ses rôles et ses groupes ;
- l'action (`SEARCH`, `REINDEX`, `SYNC`, `EVAL`) et son résultat ;
- le nombre de résultats, la confiance du premier hit et la durée ;
- une **empreinte SHA-256** de la requête.

Il ne stocke **jamais** :

- le texte brut de la requête ;
- les valeurs de nom, prénom, date ou lieu ;
- les documents Lucene.

L'écriture d'audit est « best effort » : une panne d'audit est journalisée mais
ne bloque pas la recherche. Le journal n'est ni signé ni immuable ; un
administrateur de base peut le modifier. Voir [Limites](limitations.md).

## 9. Évaluation synthétique

```bash
curl -u amina.benali@fastsearch.test:Amina-Analyste-2026 -X POST \
  'http://localhost:8080/api/v1/eval/run?seed=42&count=24'
```

L'évaluation construit un **index temporaire**, jamais l'index de production, et
calcule :

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

## 10. Tests et qualité

```bash
cd backend
mvn verify
```

La suite couvre notamment :

- normalisation des noms et dictionnaire ;
- clés phonétiques et variantes ;
- analyse et rapprochement des dates ;
- scoring et niveaux de confiance ;
- pagination stable et pages hors limites ;
- synchronisation JDBC et suppressions ;
- évaluation synthétique ;
- sécurité, rôles, groupes et CORS ;
- audit sans requête brute ;
- API REST et validation ;
- règles ArchUnit.

**État vérifié pour la version 0.1.0 : 65 tests, 0 échec.**

La CI GitHub Actions exécute `mvn verify` sur Java 17 à chaque push et pull
request, puis publie le JAR exécutable comme artefact.

## 11. Docker

Depuis la racine du monorepo, démarrer PostgreSQL et le backend seuls :

```bash
docker compose up --build postgres backend
```

Pour la stack complète, frontend inclus :

```bash
docker compose up --build
```

Le `Dockerfile` backend est multi-étapes :

1. build Maven avec `maven:3.9-eclipse-temurin-17` ;
2. image d'exécution `eclipse-temurin:17-jre-alpine` ;
3. utilisateur non privilégié `fastsearch` ;
4. healthcheck sur `/actuator/health`.

Voir [Déploiement](deployment.md) pour les volumes, les variables et la
checklist de production.

## 12. Limites et licence

Les limites détaillées sont dans [Limites connues](limitations.md). En résumé :
le dictionnaire et les règles phonétiques sont des heuristiques à curer, les
homonymes exigent des corroborations, HTTP Basic doit être placé derrière TLS,
et les comptes de démonstration doivent être remplacés.

Le projet est publié sous licence **MIT**. Voir [LICENSE](../LICENSE).

Copyright (c) 2026 FastSearch Persons contributors.
