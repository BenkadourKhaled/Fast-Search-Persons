package com.fastsearch.persons.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Extrait l'identité, les rôles et les groupes de l'utilisateur authentifié.
 * Les groupes ne viennent jamais du corps de la requête.
 */
@Component
public class CurrentUser {

    public String username(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "anonymous";
        }
        return authentication.getName();
    }

    public Set<String> roles(Authentication authentication) {
        return authorities(authentication, "ROLE_");
    }

    public Set<String> groups(Authentication authentication) {
        return authorities(authentication, "GROUP_");
    }

    private static Set<String> authorities(Authentication authentication, String prefix) {
        Set<String> result = new LinkedHashSet<>();
        if (authentication == null) {
            return result;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith(prefix) && value.length() > prefix.length()) {
                result.add(value.substring(prefix.length()));
            }
        }
        return result;
    }
}
