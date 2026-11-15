package com.fastsearch.persons.eval;

import com.fastsearch.persons.names.NameDictionary;
import com.fastsearch.persons.names.NameNormalizer;
import com.fastsearch.persons.names.PhoneticKey;
import com.fastsearch.persons.search.PersonRecord;
import com.fastsearch.persons.search.SearchRequest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Propose des entrées de dictionnaire à partir des cas où la graphie de requête
 * est inconnue mais phonétiquement proche d'un nom attendu. Aucune donnée
 * réelle : les suggestions portent sur le jeu synthétique.
 */
public final class DictionarySuggestionEngine {

    private final NameDictionary dictionary;

    public DictionarySuggestionEngine(NameDictionary dictionary) {
        this.dictionary = dictionary;
    }

    public List<DictionarySuggestion> suggest(SyntheticDataset dataset) {
        Map<String, PersonRecord> byId = new HashMap<>();
        for (PersonRecord person : dataset.persons()) {
            byId.put(person.id(), person);
        }
        Map<String, MutableSuggestion> suggestions = new HashMap<>();
        for (EvalCase evalCase : dataset.cases()) {
            PersonRecord expected = evalCase.expectedIds().stream()
                    .map(byId::get).filter(p -> p != null).findFirst().orElse(null);
            if (expected == null) {
                continue;
            }
            for (String observed : queryTokens(evalCase.request())) {
                if (dictionary.lookup(observed).isPresent()) {
                    continue;
                }
                for (String canonical : NameNormalizer.tokensForKeys(expected.fullName())) {
                    if (observed.equalsIgnoreCase(canonical)) {
                        continue;
                    }
                    PhoneticKey left = PhoneticKey.of(observed);
                    PhoneticKey right = PhoneticKey.of(canonical);
                    if (left.fine().length() < 3 || right.fine().length() < 3) {
                        continue;
                    }
                    if (left.fine().equals(right.fine())
                            || levenshtein(left.fine(), right.fine()) <= 1) {
                        String key = canonical + "|" + observed;
                        suggestions.computeIfAbsent(key,
                                k -> new MutableSuggestion(canonical, observed)).count++;
                    }
                }
            }
        }
        List<DictionarySuggestion> result = new ArrayList<>();
        suggestions.values().stream()
                .sorted(Comparator.comparingInt((MutableSuggestion s) -> s.count).reversed()
                        .thenComparing(s -> s.canonical))
                .limit(20)
                .forEach(s -> result.add(new DictionarySuggestion(s.canonical, s.observed, s.count,
                        s.canonical + ";" + s.observed)));
        return result;
    }

    private static List<String> queryTokens(SearchRequest request) {
        String text = request.q().isBlank()
                ? request.lastName() + " " + request.firstName()
                : request.q();
        List<String> tokens = new ArrayList<>();
        for (String token : NameNormalizer.tokens(text)) {
            if (!NameNormalizer.isParticle(token) && !token.matches("\\d+")) {
                tokens.add(token);
            }
        }
        return tokens;
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

    private static final class MutableSuggestion {
        private final String canonical;
        private final String observed;
        private int count;

        private MutableSuggestion(String canonical, String observed) {
            this.canonical = canonical;
            this.observed = observed;
        }
    }
}
