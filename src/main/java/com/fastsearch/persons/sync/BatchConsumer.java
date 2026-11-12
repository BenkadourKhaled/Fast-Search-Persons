package com.fastsearch.persons.sync;

/** Consommateur d'un lot source ; peut écrire dans l'index et committer. */
@FunctionalInterface
public interface BatchConsumer {

    void accept(SourceBatch batch) throws Exception;
}
