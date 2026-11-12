package com.fastsearch.persons.api;

import com.fastsearch.persons.search.IndexManager;

public record IndexStatsDto(long numDocs, long maxDoc, long deletedDocs, String path, boolean open) {

    public static IndexStatsDto from(IndexManager.IndexStats stats) {
        return new IndexStatsDto(stats.numDocs(), stats.maxDoc(), stats.deletedDocs(),
                stats.path(), stats.open());
    }
}
