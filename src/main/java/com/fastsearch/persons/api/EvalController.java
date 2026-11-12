package com.fastsearch.persons.api;

import com.fastsearch.persons.audit.AuditService;
import com.fastsearch.persons.config.FastSearchProperties;
import com.fastsearch.persons.eval.EvalReport;
import com.fastsearch.persons.eval.EvalService;
import com.fastsearch.persons.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Évaluation synthétique, réservée à ADMIN et ANALYST. */
@RestController
@RequestMapping("/api/v1/eval")
@PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
public class EvalController {

    private final EvalService evalService;
    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final FastSearchProperties properties;

    public EvalController(EvalService evalService, AuditService auditService,
                          CurrentUser currentUser, FastSearchProperties properties) {
        this.evalService = evalService;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.properties = properties;
    }

    @PostMapping("/run")
    public EvalReport run(@RequestParam(required = false) Long seed,
                          @RequestParam(required = false) Integer count,
                          Authentication authentication) {
        long effectiveSeed = seed == null ? properties.getEval().getSeed() : seed;
        int requested = count == null ? properties.getEval().getCount() : count;
        int effectiveCount = Math.max(12, Math.min(120, requested));
        EvalReport report = evalService.run(effectiveSeed, effectiveCount);
        try {
            auditService.recordAction(currentUser.username(authentication),
                    currentUser.roles(authentication), currentUser.groups(authentication),
                    "EVAL", "OK", "seed=" + effectiveSeed + ";count=" + effectiveCount);
        } catch (RuntimeException ignored) {
            // L'audit ne bloque pas l'évaluation.
        }
        return report;
    }
}
