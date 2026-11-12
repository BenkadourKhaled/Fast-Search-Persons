package com.fastsearch.persons.sync;

import com.fastsearch.persons.search.IndexManager;
import com.fastsearch.persons.search.PersonRecord;

import java.io.IOException;
import java.util.List;

/** Adaptateur entre le synchroniseur et l'IndexManager Lucene. */
public final class LuceneIndexSink implements IndexSink {

    private final IndexManager indexManager;

    public LuceneIndexSink(IndexManager indexManager) {
        this.indexManager = indexManager;
    }

    @Override
    public void clear() throws IOException {
        indexManager.deleteAll();
    }

    @Override
    public void upsertAll(List<PersonRecord> persons) throws IOException {
        indexManager.indexAll(persons);
    }

    @Override
    public void deleteAllByIds(List<String> ids) throws IOException {
        for (String id : ids) {
            indexManager.delete(id);
        }
    }

    @Override
    public void commit() throws IOException {
        indexManager.commit();
    }
}
