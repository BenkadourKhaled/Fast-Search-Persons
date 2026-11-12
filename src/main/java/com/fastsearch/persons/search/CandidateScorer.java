package com.fastsearch.persons.search;

import com.fastsearch.persons.dates.DateMatchLevel;
import com.fastsearch.persons.dates.DateMatchResult;
import com.fastsearch.persons.dates.DateMatcher;
import com.fastsearch.persons.dates.DatePrecision;
import com.fastsearch.persons.dates.ParsedDate;
import com.fastsearch.persons.names.NameDictionary;
import com.fastsearch.persons.names.NameHypothesis;
import com.fastsearch.persons.names.NameNormalizer;
import com.fastsearch.persons.names.PhoneticKey;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reranker explicite : le score final combine nom, date et lieu, avec des
 * explications par champ. Une correspondance uniquement sur la clé grossière
 * ne peut jamais atteindre FORT. Un pont arabe/latin confirmé par le
 * dictionnaire le peut, si la date est compatible (ou absente).
 */
public final class CandidateScorer {

    private static final double STRONG_NAME = 0.85;
    private static final double DATE_COMPATIBLE = 0.55;

    private final NameDictionary dictionary;
    private final DateMatcher dateMatcher;
    private final boolean enableCoarse;

    public CandidateScorer(NameDictionary dictionary, DateMatcher dateMatcher, boolean enableCoarse) {
        this.dictionary = dictionary;
        this.dateMatcher = dateMatcher == null ? new DateMatcher() : dateMatcher;
        this.enableCoarse = enableCoarse;
    }

    public ScoreResult score(ParsedSearchQuery query, PersonRecord candidate, float luceneScore) {
        boolean idMatch = query.hasId() && query.idQuery().equals(candidate.id());
        BestName best = query.hasNames() ? bestName(query, candidate) : null;
        double nameScore = best == null ? 0.0 : best.match().score();
        boolean singleTokenQuery = query.names().stream()
                .mapToInt(h -> NameNormalizer.tokens(h.fullName()).size())
                .max().orElse(0) <= 1;
        boolean incompleteSurname = singleTokenQuery && particleOnlySurname(candidate);
        if (incompleteSurname) {
            // « Ould » seul est un marqueur de filiation, pas un nom de famille.
            // Avec un seul jeton de requête, ce candidat ne peut pas être FORT.
            nameScore = Math.min(nameScore, 0.80);
        }
        boolean coarseUsed = best != null && best.match().coarseUsed();
        boolean dictionaryConfirmed = best != null && best.match().dictionaryConfirmed();
        boolean crossScript = best != null && best.match().crossScript();

        DateMatchResult dateResult = null;
        double dateScore = 0.0;
        if (query.hasDates()) {
            ParsedDate candidateDate = candidateDate(candidate);
            if (candidateDate == null) {
                dateResult = DateMatchResult.none("date de naissance absente du candidat");
            } else {
                for (ParsedDate queryDate : query.dates()) {
                    DateMatchResult result = dateMatcher.match(queryDate, candidateDate);
                    if (dateResult == null || result.score() > dateResult.score()) {
                        dateResult = result;
                    }
                }
                dateScore = dateResult == null ? 0.0 : dateResult.score();
            }
        }

        double placeScore = placeScore(query.birthPlace(), candidate.birthPlace());
        List<FieldExplanation> explanations = new ArrayList<>();
        if (best != null) {
            explanations.addAll(best.explanations());
        }
        if (idMatch) {
            explanations.add(new FieldExplanation("identifiant", "EXACT",
                    "identifiant interne exact", 1.0));
        }
        if (incompleteSurname) {
            explanations.add(new FieldExplanation("nom de famille", "INCOMPLET",
                    "nom de famille réduit à une particule de filiation : confiance plafonnée à MOYEN",
                    0.80));
        }
        if (query.hasDates()) {
            DateMatchResult result = dateResult == null
                    ? DateMatchResult.none("date absente") : dateResult;
            explanations.add(new FieldExplanation("date de naissance", result.level().name(),
                    result.detail(), result.score()));
        }
        if (!query.birthPlace().isBlank()) {
            String level = placeScore >= 1.0 ? "EXACT"
                    : placeScore >= 0.65 ? "PARTIEL"
                    : placeScore > 0 ? "FLOU" : "AUCUN";
            explanations.add(new FieldExplanation("lieu de naissance", level,
                    placeScore > 0 ? "comparaison du lieu normalisé" : "lieu incompatible", placeScore));
        }

        double finalScore;
        if (idMatch) {
            finalScore = 1.0;
        } else if (best == null) {
            finalScore = Math.min(0.45, dateScore * 0.45 + placeScore * 0.05);
        } else if (query.hasDates()) {
            if (query.birthPlace().isBlank()) {
                finalScore = 0.75 * nameScore + 0.25 * dateScore;
            } else {
                finalScore = 0.75 * nameScore + 0.20 * dateScore + 0.05 * placeScore;
            }
        } else if (query.birthPlace().isBlank()) {
            finalScore = nameScore;
        } else {
            finalScore = 0.95 * nameScore + 0.05 * placeScore;
        }
        finalScore = Math.max(0.0, Math.min(1.0, finalScore));

        Confidence confidence = confidence(query, best, nameScore, dateScore, coarseUsed,
                dictionaryConfirmed, crossScript, idMatch);
        return new ScoreResult(finalScore, confidence, explanations, nameScore, dateScore,
                placeScore, coarseUsed, dictionaryConfirmed, crossScript, idMatch);
    }

    private Confidence confidence(ParsedSearchQuery query, BestName best, double nameScore,
                                  double dateScore, boolean coarseUsed,
                                  boolean dictionaryConfirmed, boolean crossScript,
                                  boolean idMatch) {
        if (idMatch) {
            return Confidence.FORT;
        }
        if (best == null) {
            return Confidence.FAIBLE;
        }
        boolean strong = best.match().strong();
        boolean crossWithoutDictionary = crossScript && !dictionaryConfirmed;
        if (query.hasDates()) {
            boolean compatible = dateScore >= DATE_COMPATIBLE;
            if (!compatible) {
                return nameScore >= 0.75 ? Confidence.MOYEN : Confidence.FAIBLE;
            }
            if (strong && !coarseUsed && !crossWithoutDictionary && nameScore >= 0.90) {
                return Confidence.FORT;
            }
            if (nameScore >= 0.65 || (dateScore >= 0.65 && nameScore >= 0.45)) {
                return Confidence.MOYEN;
            }
            return Confidence.FAIBLE;
        }
        if (strong && !coarseUsed && !crossWithoutDictionary && nameScore >= 0.95) {
            return Confidence.FORT;
        }
        if (nameScore >= 0.65) {
            return Confidence.MOYEN;
        }
        return Confidence.FAIBLE;
    }

    private BestName bestName(ParsedSearchQuery query, PersonRecord candidate) {
        BestName best = null;
        for (NameHypothesis hypothesis : query.names()) {
            CandidateNameMatch match = matchHypothesis(hypothesis, candidate);
            if (best == null || match.match().score() > best.match().score()) {
                best = new BestName(match.match(), match.explanations(),
                        NameNormalizer.tokensForKeys(hypothesis.fullName()).size());
            }
        }
        return best;
    }

    private CandidateNameMatch matchHypothesis(NameHypothesis hypothesis, PersonRecord candidate) {
        String lastName = hypothesis.lastName();
        String firstName = hypothesis.firstName();
        if (!lastName.isBlank() && !firstName.isBlank()) {
            NameGroupMatch alignedLast = matchGroup(lastName, candidate.lastName());
            NameGroupMatch alignedFirst = matchGroup(firstName, candidate.firstName());
            double aligned = weighted(alignedLast, alignedFirst, lastName, firstName);

            NameGroupMatch crossLast = matchGroup(lastName, candidate.firstName());
            NameGroupMatch crossFirst = matchGroup(firstName, candidate.lastName());
            double crossed = weighted(crossLast, crossFirst, lastName, firstName);

            NameGroupMatch full = matchGroup(hypothesis.fullName(), candidate.fullName());

            if (full.score() > aligned && full.score() > crossed) {
                return new CandidateNameMatch(full,
                        List.of(explain("nom complet", full, "comparaison des deux ordres")));
            }
            if (crossed > aligned) {
                return new CandidateNameMatch(
                        new NameGroupMatch(crossed, worst(crossLast.level(), crossFirst.level()),
                                crossLast.strong() && crossFirst.strong(),
                                crossLast.coarseUsed() || crossFirst.coarseUsed(),
                                crossLast.dictionaryConfirmed() || crossFirst.dictionaryConfirmed(),
                                crossLast.crossScript() || crossFirst.crossScript(),
                                concat(crossLast, crossFirst)),
                        List.of(explain("nom", crossLast, "inversion nom/prénom prise en compte"),
                                explain("prénom", crossFirst, "inversion nom/prénom prise en compte")));
            }
            return new CandidateNameMatch(
                    new NameGroupMatch(aligned, worst(alignedLast.level(), alignedFirst.level()),
                            alignedLast.strong() && alignedFirst.strong(),
                            alignedLast.coarseUsed() || alignedFirst.coarseUsed(),
                            alignedLast.dictionaryConfirmed() || alignedFirst.dictionaryConfirmed(),
                            alignedLast.crossScript() || alignedFirst.crossScript(),
                            concat(alignedLast, alignedFirst)),
                    List.of(explain("nom", alignedLast, "comparaison directe"),
                            explain("prénom", alignedFirst, "comparaison directe")));
        }
        NameGroupMatch full = matchGroup(hypothesis.fullName(), candidate.fullName());
        if (full.score() == 0) {
            NameGroupMatch last = matchGroup(hypothesis.fullName(), candidate.lastName());
            NameGroupMatch first = matchGroup(hypothesis.fullName(), candidate.firstName());
            if (last.score() >= first.score() && last.score() > 0) {
                full = last;
            } else if (first.score() > 0) {
                full = first;
            }
        }
        return new CandidateNameMatch(full, List.of(explain("nom", full, "comparaison du nom")));
    }

    private NameGroupMatch matchGroup(String queryName, String candidateName) {
        List<String> queryTokens = NameNormalizer.tokensForKeys(queryName);
        List<String> candidateTokens = NameNormalizer.tokensForKeys(candidateName);
        if (queryTokens.isEmpty() || candidateTokens.isEmpty()) {
            return NameGroupMatch.none();
        }
        String queryNormalized = NameNormalizer.normalizeToken(queryName);
        String candidateNormalized = NameNormalizer.normalizeToken(candidateName);
        if (!queryNormalized.isBlank() && queryNormalized.equals(candidateNormalized)) {
            return exactGroup(queryTokens, candidateTokens);
        }
        if (dictionary.samePhraseEntry(queryName, candidateName)) {
            TokenMatch match = new TokenMatch(queryName, candidateName, NameMatchLevel.TRANSLITTERE,
                    1.0, true, crossScript(queryName, candidateName));
            return new NameGroupMatch(1.0, NameMatchLevel.TRANSLITTERE, true, false,
                    true, match.crossScript(), List.of(match));
        }

        List<TokenMatch> matches = new ArrayList<>();
        boolean allStrong = true;
        boolean coarseUsed = false;
        boolean dictionaryConfirmed = false;
        boolean cross = false;
        NameMatchLevel worst = NameMatchLevel.EXACT;
        for (String queryToken : queryTokens) {
            TokenMatch best = null;
            for (String candidateToken : candidateTokens) {
                TokenMatch current = matchToken(queryToken, candidateToken);
                if (best == null || current.score() > best.score()) {
                    best = current;
                }
            }
            if (best == null) {
                best = new TokenMatch(queryToken, "", NameMatchLevel.AUCUN, 0, false, false);
            }
            matches.add(best);
            if (best.score() < STRONG_NAME) {
                allStrong = false;
            }
            if (best.level() == NameMatchLevel.PHONETIQUE_GROSSIERE
                    || best.level() == NameMatchLevel.FLOU) {
                coarseUsed = true;
            }
            dictionaryConfirmed |= best.dictionaryConfirmed();
            cross |= best.crossScript();
            worst = worst(worst, best.level());
        }
        double score = matches.stream().mapToDouble(TokenMatch::score).average().orElse(0);
        return new NameGroupMatch(score, worst, allStrong && matches.stream().allMatch(m -> m.score() > 0),
                coarseUsed, dictionaryConfirmed, cross, matches);
    }

    private NameGroupMatch exactGroup(List<String> queryTokens, List<String> candidateTokens) {
        List<TokenMatch> matches = new ArrayList<>();
        for (String queryToken : queryTokens) {
            matches.add(new TokenMatch(queryToken, queryToken, NameMatchLevel.EXACT, 1.0,
                    false, false));
        }
        return new NameGroupMatch(1.0, NameMatchLevel.EXACT, true, false, false, false, matches);
    }

    private TokenMatch matchToken(String queryToken, String candidateToken) {
        String query = NameNormalizer.normalizeToken(queryToken);
        String candidate = NameNormalizer.normalizeToken(candidateToken);
        if (query.isBlank() || candidate.isBlank()) {
            return new TokenMatch(queryToken, candidateToken, NameMatchLevel.AUCUN, 0, false, false);
        }
        boolean cross = crossScript(queryToken, candidateToken);
        if (query.equals(candidate)) {
            return new TokenMatch(queryToken, candidateToken, NameMatchLevel.EXACT, 1.0, false, cross);
        }
        if (dictionary.sameEntry(queryToken, candidateToken)) {
            // Une entrée de dictionnaire est une équivalence curatorée : elle vaut
            // une correspondance exacte, y compris entre écritures arabe et latine.
            return new TokenMatch(queryToken, candidateToken, NameMatchLevel.TRANSLITTERE, 1.0,
                    true, cross);
        }
        PhoneticKey queryKey = PhoneticKey.of(queryToken);
        PhoneticKey candidateKey = PhoneticKey.of(candidateToken);
        if (!queryKey.fine().isBlank() && queryKey.fine().equals(candidateKey.fine())) {
            return new TokenMatch(queryToken, candidateToken, NameMatchLevel.PHONETIQUE_FINE, 0.85,
                    false, cross);
        }
        if (enableCoarse && !queryKey.coarse().isBlank()
                && queryKey.coarse().equals(candidateKey.coarse())) {
            return new TokenMatch(queryToken, candidateToken, NameMatchLevel.PHONETIQUE_GROSSIERE,
                    0.55, false, cross);
        }
        if (queryKey.fine().length() >= 3 && candidateKey.fine().length() >= 3
                && levenshtein(queryKey.fine(), candidateKey.fine()) <= 1) {
            return new TokenMatch(queryToken, candidateToken, NameMatchLevel.FLOU, 0.50, false, cross);
        }
        double similarity = trigramSimilarity(query, candidate);
        if (similarity >= 0.50) {
            return new TokenMatch(queryToken, candidateToken, NameMatchLevel.FLOU,
                    0.35 * similarity, false, cross);
        }
        return new TokenMatch(queryToken, candidateToken, NameMatchLevel.AUCUN, 0, false, cross);
    }

    private static FieldExplanation explain(String field, NameGroupMatch match, String context) {
        return new FieldExplanation(field, match.level().name(),
                context + " : " + label(match.level()), match.score());
    }

    private static String label(NameMatchLevel level) {
        return switch (level) {
            case EXACT -> "exact";
            case TRANSLITTERE -> "translittéré / variante de dictionnaire";
            case PHONETIQUE_FINE -> "phonétique fine";
            case PHONETIQUE_GROSSIERE -> "phonétique grossière (plafonnée)";
            case FLOU -> "flou toléré";
            case AUCUN -> "aucune correspondance";
        };
    }

    private static double weighted(NameGroupMatch first, NameGroupMatch second,
                                   String firstText, String secondText) {
        int firstTokens = Math.max(1, NameNormalizer.tokensForKeys(firstText).size());
        int secondTokens = Math.max(1, NameNormalizer.tokensForKeys(secondText).size());
        return (first.score() * firstTokens + second.score() * secondTokens) / (firstTokens + secondTokens);
    }

    private static List<TokenMatch> concat(NameGroupMatch first, NameGroupMatch second) {
        List<TokenMatch> all = new ArrayList<>(first.tokenMatches());
        all.addAll(second.tokenMatches());
        return all;
    }

    private static NameMatchLevel worst(NameMatchLevel left, NameMatchLevel right) {
        return rank(left) >= rank(right) ? left : right;
    }

    private static int rank(NameMatchLevel level) {
        return switch (level) {
            case EXACT -> 0;
            case TRANSLITTERE -> 1;
            case PHONETIQUE_FINE -> 2;
            case PHONETIQUE_GROSSIERE -> 3;
            case FLOU -> 4;
            case AUCUN -> 5;
        };
    }

    private static boolean crossScript(String left, String right) {
        return NameNormalizer.containsArabic(left) != NameNormalizer.containsArabic(right);
    }

    private static boolean particleOnlySurname(PersonRecord candidate) {
        return !candidate.lastName().isBlank()
                && NameNormalizer.tokensForKeys(candidate.lastName()).isEmpty();
    }

    private static double placeScore(String queryPlace, String candidatePlace) {
        if (queryPlace == null || queryPlace.isBlank()) {
            return 0.0;
        }
        String query = NameNormalizer.normalizeToken(queryPlace);
        String candidate = NameNormalizer.normalizeToken(candidatePlace);
        if (query.isBlank() || candidate.isBlank()) {
            return 0.0;
        }
        if (query.equals(candidate)) {
            return 1.0;
        }
        if (query.contains(candidate) || candidate.contains(query)) {
            return 0.70;
        }
        if (levenshtein(query, candidate) <= 1) {
            return 0.50;
        }
        return 0.0;
    }

    private static ParsedDate candidateDate(PersonRecord candidate) {
        if (candidate.birthDate() == null) {
            return null;
        }
        DatePrecision precision = candidate.birthPrecision() == null
                ? DatePrecision.JOUR : candidate.birthPrecision();
        return new ParsedDate(candidate.birthDate(), precision, candidate.birthFlags(), 1.0, "candidat");
    }

    private static double trigramSimilarity(String left, String right) {
        Set<String> a = trigrams(left);
        Set<String> b = trigrams(right);
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return (double) intersection.size() / union.size();
    }

    private static Set<String> trigrams(String value) {
        String text = value.replace(" ", "");
        if (text.length() < 3) {
            return Set.of();
        }
        Set<String> result = new HashSet<>();
        for (int i = 0; i + 3 <= text.length(); i++) {
            result.add(text.substring(i, i + 3));
        }
        return result;
    }

    private static int levenshtein(String left, String right) {
        if (Math.abs(left.length() - right.length()) > 1) {
            return 2;
        }
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    private record CandidateNameMatch(NameGroupMatch match, List<FieldExplanation> explanations) {
    }

    private record BestName(NameGroupMatch match, List<FieldExplanation> explanations, int tokenCount) {
    }
}
