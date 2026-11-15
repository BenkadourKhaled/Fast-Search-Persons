package com.fastsearch.persons.api;

import com.fastsearch.persons.audit.AuditService;
import com.fastsearch.persons.search.SearchRequest;
import com.fastsearch.persons.search.SearchResponse;
import com.fastsearch.persons.search.SearchService;
import com.fastsearch.persons.security.CurrentUser;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/** API de recherche : les groupes viennent de l'authentification, jamais du client. */
@RestController
@RequestMapping("/api/v1/persons")
public class PersonSearchController {

    private static final Logger LOG = LoggerFactory.getLogger(PersonSearchController.class);

    private final SearchService searchService;
    private final CurrentUser currentUser;
    private final AuditService auditService;

    public PersonSearchController(SearchService searchService, CurrentUser currentUser,
                                  AuditService auditService) {
        this.searchService = searchService;
        this.currentUser = currentUser;
        this.auditService = auditService;
    }

    @PostMapping("/search")
    public SearchApiResponse search(@Valid @RequestBody SearchApiRequest body,
                                    Authentication authentication) {
        Set<String> groups = currentUser.groups(authentication);
        SearchRequest request = body.toSearchRequest(groups);
        return execute(request, body.includeExplanation() == null || body.includeExplanation(),
                authentication);
    }

    @GetMapping("/search")
    public SearchApiResponse searchGet(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String birthDate,
            @RequestParam(required = false) String birthPlace,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "true") boolean explain,
            Authentication authentication) {
        SearchRequest request = new SearchRequest(lastName, firstName, birthDate, birthPlace, q,
                currentUser.groups(authentication), page, size, explain);
        return execute(request, explain, authentication);
    }

    private SearchApiResponse execute(SearchRequest request, boolean includeExplanation,
                                      Authentication authentication) {
        long started = System.nanoTime();
        SearchResponse response = searchService.search(request);
        long durationMs = (System.nanoTime() - started) / 1_000_000;
        try {
            auditService.recordSearch(currentUser.username(authentication),
                    currentUser.roles(authentication), currentUser.groups(authentication),
                    request, response, durationMs);
        } catch (RuntimeException e) {
            // L'audit ne doit pas rendre la recherche indisponible ; l'échec est visible en logs.
            LOG.warn("Écriture d'audit impossible : {}", e.getClass().getSimpleName());
        }
        return SearchApiResponse.from(response, includeExplanation);
    }
}
