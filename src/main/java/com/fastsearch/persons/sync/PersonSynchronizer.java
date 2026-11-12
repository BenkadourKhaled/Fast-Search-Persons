package com.fastsearch.persons.sync;

import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestration de la synchronisation : la base source reste la source de
 * vérité ; l'index Lucene est reconstructible. Chaque lot est écrit puis
 * committé, ce qui borne la mémoire et rend la progression visible.
 */
public final class PersonSynchronizer {

    private final JdbcPersonSource source;
    private final IndexSink sink;
    private final Clock clock;

    public PersonSynchronizer(JdbcPersonSource source, IndexSink sink, Clock clock) {
        this.source = source;
        this.sink = sink;
        this.clock = clock;
    }

    public SyncReport synchronize(SyncRequest request) {
        SyncRequest effective = request == null ? SyncRequest.full(500) : request;
        Instant started = clock.instant();
        List<String> warnings = new ArrayList<>();
        AtomicInteger batches = new AtomicInteger();
        AtomicInteger rows = new AtomicInteger();
        AtomicInteger indexed = new AtomicInteger();
        AtomicInteger deleted = new AtomicInteger();
        String[] lastId = {""};
        try {
            if (effective.full()) {
                sink.clear();
                sink.commit();
            } else if (!source.spec().supportsIncremental()) {
                throw new SyncException(
                        "Synchro incrémentale impossible : aucune colonne de mise à jour configurée");
            } else if (effective.since() == null) {
                warnings.add("date de départ absente : lecture incrémentale depuis l'origine");
            }
            Instant since = effective.full() ? null
                    : (effective.since() == null ? Instant.EPOCH : effective.since());
            source.readBatches(since, effective.batchSize(), batch -> {
                if (!batch.upserts().isEmpty()) {
                    sink.upsertAll(batch.upserts());
                    indexed.addAndGet(batch.upserts().size());
                }
                if (!batch.deletes().isEmpty()) {
                    sink.deleteAllByIds(batch.deletes());
                    deleted.addAndGet(batch.deletes().size());
                }
                sink.commit();
                batches.incrementAndGet();
                rows.addAndGet(batch.rowCount());
                lastId[0] = batch.lastId();
            });
        } catch (SQLException e) {
            throw new SyncException("Lecture de la base source impossible", e);
        } catch (SyncException e) {
            throw e;
        } catch (Exception e) {
            throw new SyncException("Synchronisation interrompue", e);
        }
        Instant finished = clock.instant();
        return new SyncReport(effective.full() ? "FULL" : "INCREMENTAL",
                started, finished, batches.get(), rows.get(), indexed.get(), deleted.get(),
                lastId[0], warnings);
    }
}
