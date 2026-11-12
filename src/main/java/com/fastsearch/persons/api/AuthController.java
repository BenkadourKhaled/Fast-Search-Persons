package com.fastsearch.persons.api;

import com.fastsearch.persons.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Point d'entrée d'identité pour le front-end. L'authentification reste HTTP
 * Basic : aucun jeton n'est émis ni stocké côté serveur.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final CurrentUser currentUser;

    public AuthController(CurrentUser currentUser) {
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public AuthUserDto me(Authentication authentication) {
        return new AuthUserDto(
                currentUser.username(authentication),
                currentUser.roles(authentication).stream().sorted().toList(),
                currentUser.groups(authentication).stream().sorted().toList());
    }
}
