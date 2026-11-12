package com.fastsearch.persons.sync;

/** Échec de synchronisation, sans exposer de données personnelles dans le message. */
public class SyncException extends RuntimeException {

    public SyncException(String message, Throwable cause) {
        super(message, cause);
    }

    public SyncException(String message) {
        super(message);
    }
}
