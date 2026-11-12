package com.fastsearch.persons.api;

import com.fastsearch.persons.search.SearchService;
import com.fastsearch.persons.sync.SyncException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Clock;
import java.time.Instant;
import java.util.stream.Collectors;

/** Erreurs API sans fuite de données personnelles ni de détail interne. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    private final Clock clock;

    public ApiExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e,
                                               HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return error(HttpStatus.BAD_REQUEST, message.isBlank() ? "requête invalide" : message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e,
                                               HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "corps JSON illisible ou type invalide", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> denied(AccessDeniedException e, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "accès refusé", request);
    }

    @ExceptionHandler(SyncException.class)
    public ResponseEntity<ApiError> sync(SyncException e, HttpServletRequest request) {
        LOG.error("Synchronisation en échec : {}", e.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, "synchronisation indisponible", request);
    }

    @ExceptionHandler(SearchService.SearchException.class)
    public ResponseEntity<ApiError> search(SearchService.SearchException e,
                                           HttpServletRequest request) {
        LOG.error("Recherche en échec : {}", e.getMessage());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "recherche indisponible", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegal(IllegalArgumentException e,
                                            HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "paramètre invalide", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest request) {
        LOG.error("Erreur API inattendue : {}", e.getClass().getSimpleName());
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "erreur interne", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message,
                                           HttpServletRequest request) {
        ApiError body = new ApiError(Instant.now(clock), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}
