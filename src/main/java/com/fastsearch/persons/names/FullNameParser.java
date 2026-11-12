package com.fastsearch.persons.names;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Découpe une chaîne libre en nom/prénom sans deviner l'ordre.
 *
 * <p>Pour n jetons, toutes les coupures sont proposées, dans les deux ordres.
 * Cela couvre l'inversion « BENALI Karim » / « Karim Benali » et les noms
 * composés. Le moteur évalue ensuite ces hypothèses ; aucune n'est déclarée
 * correcte a priori.</p>
 */
public final class FullNameParser {

    public ParsedName parse(String raw) {
        String input = raw == null ? "" : raw.trim();
        List<String> tokens = NameNormalizer.rawTokens(input);
        if (tokens.isEmpty()) {
            return new ParsedName(input, List.of(), List.of());
        }
        if (tokens.size() == 1) {
            return new ParsedName(input, tokens, List.of(new NameHypothesis(tokens.get(0), "", false)));
        }

        Set<String> seen = new LinkedHashSet<>();
        List<NameHypothesis> hypotheses = new ArrayList<>();
        int maxSplit = Math.min(tokens.size() - 1, 4);
        for (int split = 1; split <= maxSplit; split++) {
            String left = String.join(" ", tokens.subList(0, split));
            String right = String.join(" ", tokens.subList(split, tokens.size()));
            add(hypotheses, seen, new NameHypothesis(left, right, false));
            add(hypotheses, seen, new NameHypothesis(right, left, true));
        }
        // Pour les chaînes de plus de 5 jetons, au moins la coupure 1..n-1.
        if (tokens.size() > 5) {
            String left = String.join(" ", tokens.subList(0, tokens.size() - 1));
            String right = tokens.get(tokens.size() - 1);
            add(hypotheses, seen, new NameHypothesis(left, right, false));
            add(hypotheses, seen, new NameHypothesis(right, left, true));
        }
        return new ParsedName(input, tokens, hypotheses);
    }

    private static void add(List<NameHypothesis> target, Set<String> seen, NameHypothesis hypothesis) {
        if (!hypothesis.lastName().isBlank() || !hypothesis.firstName().isBlank()) {
            String key = hypothesis.lastName() + "\u0000" + hypothesis.firstName();
            if (seen.add(key)) {
                target.add(hypothesis);
            }
        }
    }
}
