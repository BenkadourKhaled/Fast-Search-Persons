package com.fastsearch.persons.sync;

import java.time.Instant;
import java.util.List;

/** Rapport de synchronisation, sans donnée personnelle. */
public record SyncReport(
        String mode,
        Instant startedAt,
        Instant finishedAt,
        int batches,
        int rowsRead,
        int indexed,
        int deleted,
        String lastId,
        List<String> warnings
) {

    public SyncReport {
        warnings = List.copyOf(warnings);
    }
}
