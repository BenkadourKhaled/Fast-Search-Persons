# Référence API

API REST **FastSearch Persons** — version `v1`.

- Base locale : `http://localhost:8080/api/v1`
- Authentification : HTTP Basic
- Format : JSON UTF-8
- Documentation interactive : <http://localhost:8080/swagger-ui.html>
- Spécification OpenAPI : <http://localhost:8080/v3/api-docs>

> [!IMPORTANT]
> L'API renvoie des **candidats**, jamais des identifications. Un score élevé
> indique une similarité, pas une certitude. La décision finale reste humaine.

## 1. Authentification et autorisation

Chaque requête métier doit inclure un en-tête HTTP Basic :

```bash
curl -u sara.haddad@fastsearch.test:Sara-Lecteur-2026 \
  http://localhost:8080/api/v1/auth/me
```

| Rôle | Accès |
|---|---|
| `READER` | Recherche, consultation de son identité |
| `ANALYST` | Recherche, évaluation synthétique |
| `ADMIN` | Recherche, administration de l'index, audit, évaluation |

Les **groupes** de lecture (`GROUP_A`, `GROUP_B`, …) sont extraits de
l'authentification. Ils ne font pas partie du contrat client et ne peuvent pas
être choisis dans la requête.

### Identité courante

```http
GET /api/v1/auth/me
```

```json
{
  "username": "sara.haddad@fastsearch.test",
  "roles": ["READER"],
  "groups": ["A"]
}
```

## 2. Endpoints

| Méthode | Chemin | Accès | Description |
|---|---|---|---|
| `GET` | `/actuator/health` | Public | Santé de l'application |
| `GET` | `/actuator/info` | Public | Informations d'application |
| `GET` | `/actuator/**` | `ADMIN` | Métriques et autres endpoints |
| `GET` | `/api/v1/auth/me` | Authentifié | Identité, rôles et groupes |
| `GET` | `/api/v1/persons/search` | `ADMIN`, `ANALYST`, `READER` | Recherche par paramètres |
| `POST` | `/api/v1/persons/search` | `ADMIN`, `ANALYST`, `READER` | Recherche structurée JSON |
| `GET` | `/api/v1/admin/index/stats` | `ADMIN` | Statistiques de l'index |
| `POST` | `/api/v1/admin/index/reindex` | `ADMIN` | Réindexation complète |
| `POST` | `/api/v1/admin/index/sync` | `ADMIN` | Synchronisation JDBC |
| `GET` | `/api/v1/admin/audit` | `ADMIN` | Journal d'audit paginé |
| `POST` | `/api/v1/eval/run` | `ADMIN`, `ANALYST` | Évaluation synthétique |

## 3. Recherche

### `GET /api/v1/persons/search`

| Paramètre | Type | Défaut | Rôle |
|---|---|---|---|
| `q` | string | — | Requête libre : nom, prénom, date, identifiant |
| `lastName` | string | — | Nom |
| `firstName` | string | — | Prénom |
| `birthDate` | string | — | Date de naissance |
| `birthPlace` | string | — | Lieu de naissance |
| `page` | integer | `0` | Page, base 0 |
| `size` | integer | `20` | Taille de page, maximum 100 |
| `explain` | boolean | `true` | Inclure les explications par champ |

Exemple texte libre :

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

### `POST /api/v1/persons/search`

Corps JSON :

| Champ | Type | Défaut | Rôle |
|---|---|---|---|
| `q` | string | — | Requête libre |
| `lastName` | string | — | Nom |
| `firstName` | string | — | Prénom |
| `birthDate` | string | — | Date de naissance |
| `birthPlace` | string | — | Lieu de naissance |
| `page` | integer | `0` | Page, base 0 |
| `size` | integer | `20` | Taille de page, maximum 100 |
| `includeExplanation` | boolean | `true` | Inclure les explications |

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

### Réponse

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
        {
          "field": "nom",
          "level": "EXACT",
          "detail": "comparaison directe : exact",
          "score": 1.0
        }
      ]
    }
  ],
  "total": 1,
  "page": 0,
  "size": 20,
  "reliable": true,
  "message": "Candidats proposés ; aucun résultat ne constitue une identification.",
  "warnings": []
}
```

### Champs de la réponse

| Champ | Type | Description |
|---|---|---|
| `hits` | array | Candidats de la page courante |
| `total` | integer | Candidats effectivement rerankés, dans la limite de `candidate-limit` |
| `page` | integer | Page effective, base 0 |
| `size` | integer | Taille de page effective |
| `reliable` | boolean | `true` si le premier candidat n'est pas `FAIBLE` et atteint le seuil de score |
| `message` | string | Message de contexte ou d'absence de résultat fiable |
| `warnings` | array | Avertissements sur la requête ou le résultat |

### Champs d'un candidat

| Champ | Description |
|---|---|
| `id` | Identifiant source |
| `lastName`, `firstName`, `fullName` | Nom, prénom et nom complet |
| `birthDate` | Date ISO `yyyy-MM-dd`, ou `null` |
| `birthPrecision` | `JOUR`, `MOIS`, `ANNEE` ou `null` |
| `birthPlace` | Lieu de naissance |
| `fatherName`, `motherName` | Noms des parents |
| `ownerGroup` | Groupe de lecture |
| `lang` | Langue de l'enregistrement (`ar`, `fr`) |
| `score` | Score métier normalisé entre 0 et 1 |
| `confidence` | `FORT`, `MOYEN` ou `FAIBLE` |
| `explanations` | Détail par champ : `field`, `level`, `detail`, `score` |

## 4. Pagination

Le contrat est côté serveur :

- `page` est indexé à partir de **0** ;
- `size` est borné par `fastsearch.search.max-page-size` (100 par défaut) ;
- `total` compte les candidats effectivement rerankés, dans la limite de
  `candidate-limit` (500 par défaut) ;
- une page hors limites est ramenée à la dernière page disponible ;
- la réponse renvoie la page **effective**, pas nécessairement la page demandée.

```bash
curl -u sara.haddad@fastsearch.test:Sara-Lecteur-2026 \
  'http://localhost:8080/api/v1/persons/search?q=Benali&page=0&size=10'
```

L'interface convertit la page 0-based en page 1-based et propose 10, 20, 50 ou
100 résultats par page, avec navigation numérotée et boutons précédent/suivant.

## 5. Confiance

| Niveau | Signification |
|---|---|
| `FORT` | Nom fort (exact, dictionnaire ou phonétique fine non grossière), aucun indice contradictoire, date compatible si fournie. |
| `MOYEN` | Rapprochement partiel, phonétique grossière, date seule ou corroboration insuffisante. |
| `FAIBLE` | Similarité faible ou aucun nom exploitable. |

Règles importantes :

- une correspondance uniquement basée sur la clé phonétique **grossière** ne
  dépasse jamais `MOYEN` ;
- une date seule, sans nom exploitable, produit `FAIBLE` ;
- un nom de famille réduit à une particule avec une requête à un seul jeton est
  plafonné à `MOYEN` ;
- `reliable = true` seulement si le premier candidat n'est pas `FAIBLE` et que
  son score atteint `0.60`.

## 6. Administration de l'index

### Statistiques

```http
GET /api/v1/admin/index/stats
```

```json
{
  "numDocs": 196,
  "maxDoc": 196,
  "deletedDocs": 0,
  "path": "./data/index",
  "open": true
}
```

### Réindexation complète

```bash
curl -u benkkadour.khaled@gmail.com:KHALED123 -X POST \
  http://localhost:8080/api/v1/admin/index/reindex
```

### Synchronisation

```bash
# Incrémentale depuis une date ISO-8601
curl -u benkkadour.khaled@gmail.com:KHALED123 -X POST \
  'http://localhost:8080/api/v1/admin/index/sync?full=false&since=2026-01-01T00:00:00Z&batchSize=500'
```

| Paramètre | Type | Défaut | Rôle |
|---|---|---|---|
| `full` | boolean | `false` | `true` = reconstruction complète |
| `since` | ISO-8601 | — | Date de début en mode incrémental |
| `batchSize` | integer | `500` | Taille de lot JDBC |

Réponse :

```json
{
  "mode": "INCREMENTAL",
  "startedAt": "2026-11-15T10:00:00Z",
  "finishedAt": "2026-11-15T10:00:02Z",
  "batches": 1,
  "rowsRead": 196,
  "indexed": 196,
  "deleted": 0,
  "lastId": "D196",
  "warnings": []
}
```

Une erreur de synchronisation renvoie `503 Service Unavailable` avec un message
générique, sans détail interne.

### Journal d'audit

```http
GET /api/v1/admin/audit?page=0&size=50
```

```json
{
  "content": [
    {
      "id": 42,
      "occurredAt": "2026-11-15T10:00:00Z",
      "username": "sara.haddad@fastsearch.test",
      "roles": ["READER"],
      "groups": ["A"],
      "action": "SEARCH",
      "queryHash": "9f2c…",
      "queryKind": "FREE",
      "resultCount": 13,
      "topConfidence": "FORT",
      "durationMs": 12,
      "outcome": "OK",
      "details": ""
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 1,
  "totalPages": 1
}
```

Le journal ne contient **jamais** le texte brut de la requête ni les valeurs de
nom, prénom, date ou lieu.

## 7. Évaluation synthétique

```bash
curl -u amina.benali@fastsearch.test:Amina-Analyste-2026 -X POST \
  'http://localhost:8080/api/v1/eval/run?seed=42&count=24'
```

| Paramètre | Type | Défaut | Rôle |
|---|---|---|---|
| `seed` | long | `42` | Graine du jeu déterministe |
| `count` | integer | `40` | Taille de base, bornée entre 12 et 120 |

Le rapport contient les métriques Recall@10, MRR, nDCG@10, précision@1, le taux
de faux `FORT`, les métriques par catégorie, les baselines et des suggestions de
dictionnaire. L'évaluation utilise un index temporaire, jamais l'index de
production.

## 8. Erreurs

Format :

```json
{
  "timestamp": "2026-11-15T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "size: doit être inférieur ou égal à 100",
  "path": "/api/v1/persons/search"
}
```

| Code | Signification |
|---|---|
| `400` | Requête invalide, champ trop long, type invalide ou JSON mal formé |
| `401` | Authentification absente ou invalide |
| `403` | Rôle insuffisant ou accès refusé |
| `404` | Ressource inconnue |
| `500` | Erreur interne |
| `503` | Synchronisation indisponible |

Les messages d'erreur ne contiennent ni donnée personnelle ni détail interne.

## 9. Bonnes pratiques client

- Toujours utiliser HTTPS en production.
- Ne jamais exposer les identifiants Basic dans une URL ou un journal.
- Conserver les identifiants dans `sessionStorage`, jamais dans `localStorage`.
- Utiliser `includeExplanation=false` pour les traitements de masse qui n'ont pas
  besoin des explications.
- Respecter `page` et `size` renvoyés par le serveur.
- Traiter `reliable=false` et `warnings` comme des signaux à afficher, pas comme
  des erreurs.
- Ne jamais présenter un candidat comme une identification confirmée.

## 10. Documentation associée

- [Guide backend](backend.md)
- [Architecture](architecture.md)
- [Guide frontend](frontend.md)
- [Limites connues](limitations.md)
