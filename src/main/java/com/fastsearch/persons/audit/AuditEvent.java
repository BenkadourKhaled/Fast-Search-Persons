package com.fastsearch.persons.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Journal d'audit sans donnée personnelle : l'identité de l'utilisateur, les
 * groupes, l'action et un empreinte SHA-256 de la requête. La requête brute
 * n'est jamais stockée.
 */
@Entity
@Table(name = "audit_event", indexes = {
        @Index(name = "idx_audit_occurred", columnList = "occurred_at"),
        @Index(name = "idx_audit_user", columnList = "username")
})
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(nullable = false, length = 128)
    private String username;

    @Column(nullable = false, length = 255)
    private String roles;

    @Column(nullable = false, length = 255)
    private String groups;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(name = "query_hash", nullable = false, length = 64)
    private String queryHash;

    @Column(name = "query_kind", nullable = false, length = 24)
    private String queryKind;

    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @Column(name = "top_confidence", length = 16)
    private String topConfidence;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(nullable = false, length = 24)
    private String outcome;

    @Column(length = 1000)
    private String details;

    public Long getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getGroups() {
        return groups;
    }

    public void setGroups(String groups) {
        this.groups = groups;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getQueryHash() {
        return queryHash;
    }

    public void setQueryHash(String queryHash) {
        this.queryHash = queryHash;
    }

    public String getQueryKind() {
        return queryKind;
    }

    public void setQueryKind(String queryKind) {
        this.queryKind = queryKind;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public String getTopConfidence() {
        return topConfidence;
    }

    public void setTopConfidence(String topConfidence) {
        this.topConfidence = topConfidence;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
