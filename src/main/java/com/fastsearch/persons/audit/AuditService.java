package com.fastsearch.persons.audit;

import com.fastsearch.persons.search.SearchRequest;
import com.fastsearch.persons.search.SearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Écriture d'audit. Les recherches sont tracées par empreinte de requête,
 * jamais par le texte brut : le journal ne doit pas devenir une copie de la
 * base de personnes.
 */
@Service
public class AuditService {

    private final AuditEventRepository repository;
    private final Clock clock;

    public AuditService(AuditEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSearch(String username, Set<String> roles, Set<String> groups,
                             SearchRequest request, SearchResponse response, long durationMs) {
        AuditEvent event = base(username, roles, "SEARCH");
        event.setQueryHash(hash(request));
        event.setQueryKind(kind(request));
        event.setResultCount((int) Math.min(Integer.MAX_VALUE, response.totalHits()));
        event.setTopConfidence(response.hits().isEmpty() ? ""
                : response.hits().get(0).confidence().name());
        event.setDurationMs(Math.max(0, durationMs));
        event.setOutcome(response.hits().isEmpty() ? "EMPTY"
                : response.reliable() ? "RELIABLE" : "LOW");
        event.setDetails("page=" + response.page() + ";size=" + response.size()
                + ";warnings=" + response.warnings().size());
        event.setGroups(join(groups));
        repository.save(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordAction(String username, Set<String> roles, Set<String> groups,
                             String action, String outcome, String details) {
        AuditEvent event = base(username, roles, action);
        event.setQueryHash("");
        event.setQueryKind("ACTION");
        event.setResultCount(0);
        event.setTopConfidence("");
        event.setDurationMs(0);
        event.setOutcome(outcome);
        event.setDetails(details == null ? "" : details);
        event.setGroups(join(groups));
        repository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> recent(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(200, Math.max(1, size));
        return repository.findAllByOrderByOccurredAtDesc(PageRequest.of(safePage, safeSize));
    }

    private AuditEvent base(String username, Set<String> roles, String action) {
        AuditEvent event = new AuditEvent();
        event.setOccurredAt(clock.instant());
        event.setUsername(username == null || username.isBlank() ? "anonymous" : username);
        event.setRoles(join(roles));
        event.setAction(action);
        return event;
    }

    private static String kind(SearchRequest request) {
        if (request == null) {
            return "NONE";
        }
        if (!request.q().isBlank()) {
            return "FREE";
        }
        if (!request.lastName().isBlank() || !request.firstName().isBlank()) {
            return "STRUCTURED";
        }
        if (!request.birthDate().isBlank() || !request.birthPlace().isBlank()) {
            return "DATE_OR_PLACE";
        }
        return "EMPTY";
    }

    static String hash(SearchRequest request) {
        if (request == null) {
            return "";
        }
        String canonical = String.join("\u0000",
                request.lastName(), request.firstName(), request.birthDate(),
                request.birthPlace(), request.q());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }

    private static String join(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream().sorted().collect(Collectors.joining(","));
    }
}
