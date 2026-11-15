# FastSearch Persons — Backend

Backend Java 17 / Spring Boot 3.3 / Lucene 9.12 du monorepo **FastSearch Persons**.
Il expose l'API REST `/api/v1`, applique le cloisonnement par groupes et produit
des **candidats** explicables, jamais des identifications.

> Toutes les données livrées avec le projet sont **synthétiques**. Les comptes de
> démonstration sont publics et doivent être remplacés avant tout déploiement.

## Documentation

Toute la documentation de référence est centralisée à la racine du monorepo :

- [Guide backend](../docs/backend.md)
- [Référence API](../docs/api.md)
- [Architecture](../docs/architecture.md)
- [Déploiement](../docs/deployment.md)
- [Limites connues](../docs/limitations.md)
- [README principal](../README.md)
- [Contribution](../CONTRIBUTING.md) · [Sécurité](../SECURITY.md) ·
  [Journal des modifications](../CHANGELOG.md)

## Démarrage rapide

```bash
cd backend
mvn clean package
java -jar target/fastsearch-persons.jar
```

- API : <http://localhost:8080/api/v1>
- Swagger UI : <http://localhost:8080/swagger-ui.html>
- Santé : <http://localhost:8080/actuator/health>

## Tests

```bash
cd backend
mvn verify
```

## Licence

Projet publié sous licence **MIT**. Voir [LICENSE](../LICENSE).
