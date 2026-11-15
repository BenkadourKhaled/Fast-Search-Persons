# Limites connues

Ce document décrit ce que le moteur **ne garantit pas**. Il doit être lu avant
toute mise en production ou toute interprétation des scores.

## 1. Données et évaluation

- Le projet ne contient **aucune donnée réelle**. Le jeu d'évaluation est
  synthétique et déterministe ; ses métriques ne prouvent pas la qualité sur un
  vrai registre.
- Les seuils de confiance et les poids de scoring sont **fixés manuellement**,
  pas appris sur des décisions métier. Ils doivent être calibrés sur un jeu
  anonymisé et validé par les métiers.
- Les catégories d'évaluation (exact, inversion, arabe, translittération, faute,
  date, homonyme) ne couvrent pas toutes les variations réelles : dialectes,
  erreurs OCR systématiques, noms composés, changements de nom, translittérations
  administratives multiples.
- La baseline SQL est une simulation en mémoire ; la baseline OpenSearch est
  optionnelle et n'est pas exécutée si aucun point de terminaison n'est
  configuré. Le rapport l'indique alors comme indisponible.

## 2. Noms, langues et phonétique

- Le dictionnaire embarqué est **limité et curé à la main**. Une graphie absente
  peut ne pas être reliée à ses variantes. Les suggestions d'évaluation sont des
  propositions, pas des vérités.
- Les règles phonétiques sont des heuristiques. La clé fine conserve des voyelles
  et reste sensible aux conventions de translittération (`ou`/`u`, `e`/`a`,
  `i`/`y`, lettres emphatiques, `kh`/`k`, etc.).
- La clé grossière est volontairement très collisionnelle. Elle est plafonnée à
  `MOYEN`, mais peut encore produire des candidats parasites.
- Le traitement des particules (`ben`, `bin`, `ould`, `el`, `ال`…) et des
  préfixes patronymiques est heuristique. Il peut rapprocher à tort des noms
  distincts ou masquer une partie du nom.
- L'arabe n'est pas analysé morphologiquement : pas de gestion complète des
  préfixes, suffixes, pluriels irréguliers ni de l'ordre des mots complexe.
- Les fautes prises en charge reposent sur une distance d'édition faible, les
  trigrammes et les clés phonétiques. Les erreurs OCR importantes, les
  caractères confondus en masse ou les omissions de mots ne sont pas garantis.
- Les noms très courts, les prénoms uniques et les homonymes stricts restent
  ambigus, même avec une date identique.

## 3. Dates

- Les formats ambigus (jour/mois, année à deux chiffres, zéros initiaux perdus)
  produisent des **hypothèses** explicites ; le moteur ne connaît pas
  l'intention réelle de l'utilisateur.
- Une date conventionnelle (`01/01`, `1/1`) est pénalisée par défaut.
- Une date seule ne suffit pas à produire une confiance `FORT`.
- Les dates partielles (mois ou année seulement) sont moins discriminantes.
- Une erreur d'un chiffre, une inversion jour/mois ou un décalage d'année peuvent
  être tolérés ; ils ne constituent pas une preuve d'identité.
- Les fuseaux horaires et les dates stockées comme texte dépendent de la qualité
  de la source ; la synchronisation applique des règles de lecture, pas une
  correction métier.

## 4. Recherche et classement

- Lucene fournit un vivier de candidats (limite configurable, 500 par défaut)
  ensuite reranké en mémoire. Un vrai positif absent de ce vivier ne peut pas
  être récupéré.
- Le classement final privilégie le score métier ; à score égal, l'identifiant
  source assure un ordre stable. Ce n'est pas un classement probabiliste.
- Le `total` de la pagination compte les candidats effectivement rerankés, dans
  la limite de `candidate-limit`. Il ne représente pas nécessairement tous les
  documents Lucene correspondant à la requête.
- Les explications décrivent les comparaisons effectuées, pas une causalité
  juridique ou administrative.
- Le moteur ne fait pas de déduplication d'identité : deux documents peuvent
  représenter la même personne, ou deux personnes différentes porter le même
  nom.
- Les résultats sont des **candidats**. Ils ne doivent jamais servir seuls à
  refuser un droit, une prestation ou un accès.

## 5. Synchronisation

- Une seule table source est synchronisée à la fois ; le schéma est configuré
  côté serveur.
- Le mode incrémental dépend d'une colonne `updated_at` fiable et monotone. Une
  horloge source incorrecte, une mise à jour sans changement d'horodatage ou une
  suppression physique non tracée ne seront pas vues.
- Les suppressions ne sont prises en compte que si la colonne `deleted` est
  configurée (suppression logique).
- Il n'y a **pas de verrou distribué** : deux synchronisations simultanées
  peuvent se chevaucher. En production, sérialiser les appels.
- Une reconstruction complète vide l'index avant de le remplir ; pendant cette
  fenêtre, les lecteurs peuvent voir un index partiel ou vide.
- Il n'y a pas de migrations versionnées : `ddl-auto=update` est pratique en
  démonstration, mais insuffisant pour une production rigoureuse (préférer
  Flyway ou Liquibase).
- Aucun test de charge ni de très gros volume n'a été réalisé. Les performances
  dépendent du disque, de la mémoire, du nombre de variantes et de la limite de
  candidats.

## 6. Sécurité et audit

- HTTP Basic sans TLS expose les identifiants. Un reverse proxy TLS est
  indispensable.
- Les utilisateurs sont en mémoire ou en base selon le profil, et les comptes
  livrés ont des mots de passe de démonstration. Il n'y a ni MFA, ni verrouillage
  de compte, ni rotation de secrets, ni intégration à un annuaire d'entreprise.
- Il n'y a pas de limitation de débit ni de protection anti-abus sur l'API.
- Les groupes sont portés par l'authentification, mais leur gouvernance (qui
  appartient à quel groupe) n'est pas gérée par l'application.
- L'audit stocke un **hash SHA-256** de la requête, pas le texte brut. Un hash de
  nom reste une donnée pseudonyme : il peut être attaqué par dictionnaire et doit
  être protégé comme une donnée sensible.
- L'écriture d'audit est « best effort » : une panne d'audit est journalisée mais
  ne bloque pas la recherche. Le journal n'est ni signé ni immuable ; un
  administrateur de base peut le modifier.
- Aucune politique de rétention, de purge ou d'export RGPD n'est implémentée.
  Elle doit être définie avant production.
- Actuator expose `health`, `info`, `metrics` ; les détails de santé sont masqués
  et les endpoints autres que `health`/`info` exigent `ADMIN`.
- Le frontend conserve les identifiants Basic dans `sessionStorage`, ce qui
  expose à un risque XSS. La CSP Nginx et la revue du code limitent ce risque,
  sans l'éliminer.

## 7. Déploiement

- H2 convient à la démonstration, pas à une production multi-utilisateurs.
  Utiliser PostgreSQL et une sauvegarde testée.
- L'index Lucene est local au conteneur/serveur ; le volume doit être persistant.
  Il n'y a pas de réplication ni de haute disponibilité.
- Le `docker-compose.yml` est un exemple mono-nœud avec mot de passe par défaut.
  Il n'est pas endurci pour Internet.
- Aucun benchmark, test de reprise après sinistre ou test de montée en charge
  n'est fourni.

## 8. Périmètre fonctionnel

- L'interface permet la recherche, la consultation, l'export JSON de la page
  courante, l'administration de l'index, l'audit et l'évaluation.
- Il n'y a pas de gestion de dossiers, de workflow de validation humaine, de
  gestion des conflits d'identité ni de notifications.
- Aucun endpoint de mise à jour des personnes n'est exposé : la base source
  reste gérée par les systèmes amont.
- Pas d'intégration à un système d'identité existant.
- Pas de déduplication automatique ni de fusion d'identités.

## 9. Cadre d'usage

Toute utilisation sur des données réelles exige une analyse juridique
(consentement, base légale, minimisation, durée de conservation, droits des
personnes), une politique d'accès et une validation métier des tolérances. Le
moteur est un outil d'aide à la recherche : la décision finale reste humaine et
documentée.

> Un score élevé ne signifie pas « identité confirmée ». Il signifie
> « similarité élevée entre la requête et un enregistrement ».
