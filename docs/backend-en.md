# Backend guide (English)

**FastSearch Persons** backend is a Java 17 / Spring Boot 3.3 / Lucene 9.12
application. It exposes a versioned REST API, enforces group-based access
control, audits searches as SHA-256 fingerprints and provides a reproducible
synthetic evaluation module.

> All bundled data is **synthetic**. No name, date or identifier belongs to a
> real person.

## Stack

| Item | Version / role |
|---|---|
| Java | 17 |
| Spring Boot | 3.3.13 |
| Spring MVC / Validation | REST API, DTOs, structured errors |
| Spring Security | HTTP Basic, roles, groups, BCrypt |
| Spring Data JPA / JDBC | Audit persistence, batched source reads |
| Apache Lucene | 9.12.2, embedded index |
| Database | H2 by default, PostgreSQL with the `postgres` profile |
| Tests | JUnit 5, Spring Boot Test, ArchUnit |
| Build | Maven, single `pom.xml` |
| API docs | springdoc-openapi / Swagger UI |

## Package layout

```text
backend/src/main/java/com/fastsearch/persons/
├── analysis/   Lucene analyzers (Arabic, trigrams, particles)
├── names/      normalization, dictionary, phonetics, variants
├── dates/      date parsing and matching (injected Clock)
├── search/     index, queries, scoring, explanations, pagination
├── sync/       batched JDBC reads, SQL validation, Lucene sink
├── eval/       synthetic dataset, metrics, baselines
├── api/        REST API v1, DTOs, error handling
├── config/     Spring wiring and demo data
├── security/   HTTP Basic, roles and groups
└── audit/      JPA audit log with query fingerprint
```

`analysis`, `names`, `dates` and `search` do not import Spring or JPA. An
ArchUnit test enforces this boundary.

## Local quick start

Requirements: JDK **17** and Maven **3.9+**.

```bash
cd backend
mvn clean test
mvn package
java -jar target/fastsearch-persons.jar
```

Default behavior:

- H2 file database in `./data/fastsearch`;
- Lucene index in `./data/index`;
- 196 synthetic demo people inserted at startup;
- API on `http://localhost:8080`.

Health check:

```bash
curl http://localhost:8080/actuator/health
```

To start from an empty database, stop the application and delete `./data`.

## PostgreSQL profile

```bash
export SPRING_PROFILES_ACTIVE=postgres
export FASTSEARCH_DB_URL=jdbc:postgresql://localhost:5432/fastsearch
export FASTSEARCH_DB_USER=fastsearch
export FASTSEARCH_DB_PASSWORD=change-me
java -jar target/fastsearch-persons.jar
```

The `postgres` profile enables demo data and the JDBC identity store
(`fastsearch_users`, `fastsearch_authorities`). Disable demo data with:

```bash
export FASTSEARCH_DEMO_ENABLED=false
```

The SQL export of the 196 synthetic people is available in
[`db/person_source_data.sql`](../backend/db/person_source_data.sql).

## Configuration

| Property | Default | Purpose |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `fastsearch.index.path` | `./data/index` | Lucene index directory |
| `fastsearch.dictionary-path` | empty | External CSV; empty = embedded dictionary |
| `fastsearch.search.candidate-limit` | `500` | Maximum reranked Lucene candidates |
| `fastsearch.search.max-page-size` | `100` | Maximum page size |
| `fastsearch.search.enable-coarse` | `true` | Enable coarse phonetic key |
| `fastsearch.sync.batch-size` | `500` | Default JDBC batch size |
| `fastsearch.sync.table` | `person_source` | Source table |
| `fastsearch.sync.*-column` | standard columns | Column mapping |
| `fastsearch.eval.seed` | `42` | Synthetic dataset seed |
| `fastsearch.eval.count` | `40` | Default case count (12 to 120) |
| `fastsearch.eval.open-search-url` | empty | Optional OpenSearch baseline |
| `fastsearch.demo.enabled` | `true` | Synthetic demo data at startup |
| `fastsearch.security.store` | `memory` | `memory` or `jdbc` |
| `fastsearch.security.default-users` | `false` | Fallback demo users |
| `fastsearch.security.users` | test accounts | Explicit users |
| `fastsearch.cors.allowed-origins` | Vite origins | Allowed CORS origins |

## Security

HTTP Basic, stateless sessions. Roles: `ADMIN`, `ANALYST`, `READER`. Groups:
`GROUP_xxx` authorities.

| User | Password | Roles | Groups |
|---|---|---|---|
| `benkkadour.khaled@gmail.com` | `KHALED123` | ADMIN | A, B |
| `amina.benali@fastsearch.test` | `Amina-Analyste-2026` | ANALYST | A |
| `yassine.merabet@fastsearch.test` | `Yassine-Analyste-2026` | ANALYST | B |
| `sara.haddad@fastsearch.test` | `Sara-Lecteur-2026` | READER | A |
| `karim.bouzid@fastsearch.test` | `Karim-Lecteur-2026` | READER | B |

> [!WARNING]
> These accounts are public and intended for local demonstration only. Replace
> them before any deployment. Addresses ending in `@fastsearch.test` are
> fictitious.

### Access matrix

| Method | Path | Access |
|---|---|---|
| GET | `/actuator/health`, `/actuator/info` | Public |
| GET | `/actuator/**` (others) | ADMIN |
| GET | `/api/v1/auth/me` | Authenticated |
| GET, POST | `/api/v1/persons/search` | ADMIN, ANALYST, READER |
| GET | `/api/v1/admin/index/stats` | ADMIN |
| POST | `/api/v1/admin/index/reindex` | ADMIN |
| POST | `/api/v1/admin/index/sync` | ADMIN |
| GET | `/api/v1/admin/audit` | ADMIN |
| POST | `/api/v1/eval/run` | ADMIN, ANALYST |

OpenAPI UI: <http://localhost:8080/swagger-ui.html>

## Search and pagination

```bash
curl -u sara.haddad@fastsearch.test:Sara-Lecteur-2026 \
  --get --data-urlencode 'q=Oualid Benali 07/04/1992' \
  http://localhost:8080/api/v1/persons/search
```

`page` is 0-based. `size` is capped at 100. `total` counts the candidates that
were actually reranked, within `candidate-limit`. An out-of-range page is
clamped to the last available page, and the response returns the effective
page. The UI exposes 10, 20, 50 and 100 results per page with numbered
navigation.

Confidence levels:

- `FORT`: strong name match, no contradiction, compatible date when supplied;
- `MOYEN`: partial match, coarse phonetic match, date-only or insufficient
  corroboration;
- `FAIBLE`: weak similarity or no usable name.

A coarse-phonetic-only match never exceeds `MOYEN`. A date-only query produces
`FAIBLE`.

## JDBC synchronization

The standard source table contains:

```text
id, last_name, first_name, birth_date, birth_precision, birth_flags,
birth_place, father_name, mother_name, owner_group, lang, updated_at, deleted
```

The synchronizer reads with `PreparedStatement` and `setFetchSize`, processes
batches (default 500), writes upserts and deletions to Lucene, and commits after
each batch. Table and column names are validated by regular expression and
quoted; values are always bound.

| Mode | Behavior |
|---|---|
| `FULL` | Clear the index and read the whole source |
| `INCREMENTAL` | Read rows with `updated_at >= since` |
| `INCREMENTAL` without `since` | Read from the beginning and add a warning |

```bash
# Full reindex
curl -u benkkadour.khaled@gmail.com:KHALED123 -X POST \
  http://localhost:8080/api/v1/admin/index/reindex

# Incremental sync
curl -u benkkadour.khaled@gmail.com:KHALED123 -X POST \
  'http://localhost:8080/api/v1/admin/index/sync?full=false&since=2026-01-01T00:00:00Z&batchSize=500'
```

## Audit

The audit log stores the user, roles, groups, action, outcome, result count, top
confidence, duration and a **SHA-256 fingerprint** of the query. It never stores
the raw query text or field values. Audit writes are best-effort: a failure is
logged but does not block search.

## Synthetic evaluation

```bash
curl -u amina.benali@fastsearch.test:Amina-Analyste-2026 -X POST \
  'http://localhost:8080/api/v1/eval/run?seed=42&count=24'
```

The evaluation builds a temporary index and computes Recall@10, MRR, nDCG@10,
precision@1, false-`FORT` rate, per-category metrics, a simulated SQL `LIKE`
baseline and an optional OpenSearch baseline. Dictionary suggestions are
proposals only and are never applied automatically.

## Tests

```bash
cd backend
mvn verify
```

The suite covers name normalization and dictionary, phonetic keys, dates,
scoring and confidence, stable pagination, JDBC synchronization, evaluation,
security, audit, REST validation and ArchUnit rules.

**Verified state for version 0.1.0: 65 tests, 0 failures.**

## Docker

From the monorepo root, start PostgreSQL and the backend only:

```bash
docker compose up --build postgres backend
```

For the full stack, frontend included:

```bash
docker compose up --build
```

See [Deployment](deployment.md).

## Limitations and license

See [Known limitations](limitations.md) and the root [LICENSE](../LICENSE). The
project is released under the **MIT** license.

Copyright (c) 2026 FastSearch Persons contributors.
