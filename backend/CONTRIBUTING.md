# Contributing

Merci de contribuer à FastSearch Persons.

## Développement

Prérequis : JDK 17, Maven 3.9+.

```bash
mvn clean test
mvn package
java -jar target/fastsearch-persons.jar
```

La documentation fonctionnelle et l'API sont décrites dans `README.md`,
`README_GITHUB.md` et `README_GITHUB_EN.md`. La documentation OpenAPI est
exposée sur `/swagger-ui.html` lorsque l'application tourne.

## Avant une pull request

1. Ajouter ou adapter les tests concernés.
2. Exécuter `mvn verify`.
3. Vérifier que les données de démonstration restent synthétiques.
4. Ne jamais committer de secret, de données réelles ou de fichier `target/`.
5. Décrire clairement le comportement modifié et les risques éventuels.

## Style

- Java 17, quatre espaces, pas de wildcard imports.
- Les packages `analysis`, `names`, `dates` et `search` ne doivent pas dépendre
  de Spring, de JPA ou de la couche web.
- Les messages destinés à l'utilisateur restent en français côté backend ; le
  frontend gère leur traduction à l'affichage.
- Les commits doivent rester ciblés et explicites.

## Sécurité

Les vulnérabilités doivent être signalées selon `SECURITY.md`, pas dans une
issue publique.
