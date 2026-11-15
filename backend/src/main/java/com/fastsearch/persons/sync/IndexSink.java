package com.fastsearch.persons.sync;

import com.fastsearch.persons.search.PersonRecord;

import java.io.IOException;
import java.util.List;

/** Port d'écriture d'index, implémenté par l'adaptateur Lucene. */
public interface IndexSink {

    void clear() throws IOException;

    void upsertAll(List<PersonRecord> persons) throws IOException;

    void deleteAllByIds(List<String> ids) throws IOException;

    void commit() throws IOException;
}
