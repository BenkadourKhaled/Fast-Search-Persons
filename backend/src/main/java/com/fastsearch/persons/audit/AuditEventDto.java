package com.fastsearch.persons.audit;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/** Vue API d'un événement d'audit ; aucune requête brute. */
public record AuditEventDto(
        Long id,
        Instant occurredAt,
        String username,
        List<String> roles,
        List<String> groups,
        String action,
        String queryHash,
        String queryKind,
        int resultCount,
        String topConfidence,
        long durationMs,
        String outcome,
        String details
) {

    public static AuditEventDto from(AuditEvent event) {
        return new AuditEventDto(event.getId(), event.getOccurredAt(), event.getUsername(),
                split(event.getRoles()), split(event.getGroups()), event.getAction(),
                event.getQueryHash(), event.getQueryKind(), event.getResultCount(),
                event.getTopConfidence(), event.getDurationMs(), event.getOutcome(),
                event.getDetails());
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }
}
