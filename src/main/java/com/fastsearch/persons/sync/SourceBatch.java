package com.fastsearch.persons.sync;

import com.fastsearch.persons.search.PersonRecord;

import java.util.List;

/** Un lot lu depuis la base source : mises à jour et suppressions logiques. */
public record SourceBatch(List<PersonRecord> upserts, List<String> deletes, String lastId, int rowCount) {

    public SourceBatch {
        upserts = List.copyOf(upserts);
        deletes = List.copyOf(deletes);
    }
}
