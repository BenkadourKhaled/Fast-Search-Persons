package com.fastsearch.persons.eval;

import com.fastsearch.persons.search.PersonRecord;

import java.util.List;

/** Jeu synthétique : personnes et cas, sans aucune donnée réelle. */
public record SyntheticDataset(List<PersonRecord> persons, List<EvalCase> cases) {

    public SyntheticDataset {
        persons = List.copyOf(persons);
        cases = List.copyOf(cases);
    }
}
