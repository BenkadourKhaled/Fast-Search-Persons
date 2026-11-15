package com.fastsearch.persons.api;

import com.fastsearch.persons.sync.SyncReport;

import java.time.Instant;
import java.util.List;

public record SyncReportDto(
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

    public static SyncReportDto from(SyncReport report) {
        return new SyncReportDto(report.mode(), report.startedAt(), report.finishedAt(),
                report.batches(), report.rowsRead(), report.indexed(), report.deleted(),
                report.lastId(), report.warnings());
    }
}
