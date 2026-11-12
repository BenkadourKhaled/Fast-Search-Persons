package com.fastsearch.persons.api;

import com.fastsearch.persons.audit.AuditService;
import com.fastsearch.persons.config.FastSearchProperties;
import com.fastsearch.persons.search.SearchService;
import com.fastsearch.persons.security.CurrentUser;
import com.fastsearch.persons.sync.PersonSynchronizer;
import com.fastsearch.persons.sync.SyncRequest;
import com.fastsearch.persons.sync.SyncReport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/** Administration de l'index, réservée au rôle ADMIN. */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final SearchService searchService;
    private final PersonSynchronizer synchronizer;
    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final FastSearchProperties properties;

    public AdminController(SearchService searchService, PersonSynchronizer synchronizer,
                           AuditService auditService, CurrentUser currentUser,
                           FastSearchProperties properties) {
        this.searchService = searchService;
        this.synchronizer = synchronizer;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.properties = properties;
    }

    @GetMapping("/index/stats")
    public IndexStatsDto stats() {
        return IndexStatsDto.from(searchService.stats());
    }

    @PostMapping("/index/reindex")
    public SyncReportDto reindex(Authentication authentication) {
        SyncReport report = synchronizer.synchronize(
                SyncRequest.full(properties.getSync().getBatchSize()));
        audit(authentication, "REINDEX", report);
        return SyncReportDto.from(report);
    }

    @PostMapping("/index/sync")
    public SyncReportDto sync(
            @RequestParam(defaultValue = "false") boolean full,
            @RequestParam(required = false) Instant since,
            @RequestParam(required = false) Integer batchSize,
            Authentication authentication) {
        int size = batchSize == null ? properties.getSync().getBatchSize() : batchSize;
        SyncReport report = synchronizer.synchronize(new SyncRequest(full, since, size));
        audit(authentication, "SYNC", report);
        return SyncReportDto.from(report);
    }

    @GetMapping("/audit")
    public AuditPageDto audit(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "50") int size) {
        return AuditPageDto.from(auditService.recent(page, size));
    }

    private void audit(Authentication authentication, String action, SyncReport report) {
        try {
            auditService.recordAction(currentUser.username(authentication),
                    currentUser.roles(authentication), currentUser.groups(authentication),
                    action, "OK", "indexed=" + report.indexed() + ";deleted=" + report.deleted());
        } catch (RuntimeException ignored) {
            // Voir PersonSearchController : l'audit ne bloque pas l'action.
        }
    }
}
