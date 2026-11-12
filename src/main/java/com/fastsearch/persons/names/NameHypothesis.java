package com.fastsearch.persons.names;

import java.util.List;

/** Hypothèse d'ordre nom/prénom. */
public record NameHypothesis(String lastName, String firstName, boolean inverted) {

    public NameHypothesis {
        lastName = lastName == null ? "" : lastName.trim();
        firstName = firstName == null ? "" : firstName.trim();
    }

    public String fullName() {
        if (lastName.isBlank()) {
            return firstName;
        }
        if (firstName.isBlank()) {
            return lastName;
        }
        return lastName + " " + firstName;
    }
}
