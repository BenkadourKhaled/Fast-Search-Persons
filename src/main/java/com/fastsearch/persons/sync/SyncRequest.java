package com.fastsearch.persons.sync;

import java.time.Instant;

/**
 * Demande de synchronisation.
 *
 * @param full      true : l'index est vidé puis reconstruit depuis la source
 * @param since     date de dernière synchronisation pour le mode incrémental
 * @param batchSize taille de lot, bornée à 10 000
 */
public record SyncRequest(boolean full, Instant since, int batchSize) {

    private static final int MAX_BATCH = 10_000;

    public SyncRequest {
        batchSize = batchSize <= 0 ? 500 : Math.min(batchSize, MAX_BATCH);
    }

    public static SyncRequest full(int batchSize) {
        return new SyncRequest(true, null, batchSize);
    }

    public static SyncRequest incremental(Instant since, int batchSize) {
        return new SyncRequest(false, since, batchSize);
    }
}
