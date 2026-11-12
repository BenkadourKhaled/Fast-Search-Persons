package com.fastsearch.persons.eval;

/** Résultat d'une baseline ; les métriques sont nulles si elle n'est pas disponible. */
public record BaselineResult(String name, boolean available, EvalMetrics metrics, String note) {

    public static BaselineResult available(String name, EvalMetrics metrics, String note) {
        return new BaselineResult(name, true, metrics, note);
    }

    public static BaselineResult unavailable(String name, String note) {
        return new BaselineResult(name, false, null, note);
    }
}
