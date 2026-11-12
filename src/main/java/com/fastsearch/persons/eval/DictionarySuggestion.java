package com.fastsearch.persons.eval;

/** Suggestion de ligne CSV pour enrichir le dictionnaire après évaluation. */
public record DictionarySuggestion(String canonical, String observed, int occurrences,
                                   String suggestedLine) {
}
