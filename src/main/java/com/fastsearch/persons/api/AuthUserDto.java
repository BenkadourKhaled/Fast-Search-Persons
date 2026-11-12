package com.fastsearch.persons.api;

import java.util.List;

/**
 * Identité effective de l'utilisateur authentifié. Le front-end s'en sert pour
 * afficher les bons écrans ; les groupes restent décidés par le serveur.
 */
public record AuthUserDto(String username, List<String> roles, List<String> groups) {

    public AuthUserDto {
        roles = List.copyOf(roles);
        groups = List.copyOf(groups);
    }
}
