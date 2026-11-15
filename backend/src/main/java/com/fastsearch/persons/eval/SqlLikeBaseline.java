package com.fastsearch.persons.eval;

import com.fastsearch.persons.names.NameNormalizer;
import com.fastsearch.persons.search.Confidence;
import com.fastsearch.persons.search.PersonRecord;
import com.fastsearch.persons.search.SearchRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Baseline « SQL LIKE » simulée sur le jeu synthétique : égalité ou
 * sous-chaîne normalisée, sans phonétique, sans dictionnaire et sans
 * trigrammes. Elle sert de point de comparaison, pas de moteur de production.
 */
public final class SqlLikeBaseline {

    private final List<PersonRecord> persons;

    public SqlLikeBaseline(List<PersonRecord> persons) {
        this.persons = List.copyOf(persons);
    }

    public BaselineResult evaluate(SyntheticDataset dataset, int topK) {
        int k = Math.max(1, topK);
        List<EvalOutcome> outcomes = new ArrayList<>(dataset.cases().size());
        for (EvalCase evalCase : dataset.cases()) {
            List<String> queryTokens = queryTokens(evalCase.request());
            List<ScoredPerson> matches = new ArrayList<>();
            if (!queryTokens.isEmpty()) {
                String phrase = String.join(" ", queryTokens);
                for (PersonRecord person : persons) {
                    if (!evalCase.request().ownerGroups().contains(person.ownerGroup())) {
                        continue;
                    }
                    String last = NameNormalizer.normalizeToken(person.lastName());
                    String first = NameNormalizer.normalizeToken(person.firstName());
                    String full = NameNormalizer.normalizeToken(person.fullName());
                    int matched = 0;
                    for (String token : queryTokens) {
                        if (full.contains(token) || last.contains(token) || first.contains(token)) {
                            matched++;
                        }
                    }
                    if (matched != queryTokens.size()) {
                        continue;
                    }
                    int rank;
                    if (full.equals(phrase)) {
                        rank = 0;
                    } else if (last.equals(phrase) || first.equals(phrase)
                            || (last + " " + first).equals(phrase)) {
                        rank = 1;
                    } else {
                        rank = 2;
                    }
                    matches.add(new ScoredPerson(person.id(), rank));
                }
            }
            matches.sort(Comparator.comparingInt(ScoredPerson::rank)
                    .thenComparing(ScoredPerson::id));
            List<String> ids = matches.stream().limit(k).map(ScoredPerson::id).toList();
            List<Confidence> confidences = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                confidences.add(Confidence.MOYEN);
            }
            outcomes.add(new EvalOutcome(evalCase.id(), evalCase.category(),
                    evalCase.expectedIds(), ids, confidences));
        }
        EvalMetrics metrics = MetricsCalculator.compute(outcomes, k);
        return BaselineResult.available("SQL LIKE",
                metrics, "sous-chaîne normalisée, sans phonétique ni dictionnaire");
    }

    private static List<String> queryTokens(SearchRequest request) {
        String text = request.q().isBlank()
                ? request.lastName() + " " + request.firstName()
                : request.q();
        List<String> result = new ArrayList<>();
        for (String token : NameNormalizer.tokens(text)) {
            String normalized = NameNormalizer.normalizeToken(token);
            if (normalized.isBlank() || normalized.matches("\\d+")) {
                continue;
            }
            result.add(normalized.toLowerCase(Locale.ROOT));
        }
        return result;
    }

    private record ScoredPerson(String id, int rank) {
    }
}
