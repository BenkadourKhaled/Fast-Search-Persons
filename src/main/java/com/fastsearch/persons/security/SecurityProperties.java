package com.fastsearch.persons.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilisateurs on-premise. En production, remplacer les comptes de
 * démonstration et les mots de passe par une source d'identité gérée.
 */
@ConfigurationProperties(prefix = "fastsearch.security")
public class SecurityProperties {

    private boolean defaultUsers = true;
    /**
     * {@code memory} : utilisateurs lus dans la configuration YAML.
     * {@code jdbc} : utilisateurs lus dans PostgreSQL (tables fastsearch_users
     * et fastsearch_authorities).
     */
    private String store = "memory";
    private List<User> users = new ArrayList<>();

    public boolean isDefaultUsers() {
        return defaultUsers;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store == null || store.isBlank()
                ? "memory" : store.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public void setDefaultUsers(boolean defaultUsers) {
        this.defaultUsers = defaultUsers;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users == null ? new ArrayList<>() : users;
    }

    public List<User> effectiveUsers() {
        if (!users.isEmpty()) {
            return List.copyOf(users);
        }
        if (!defaultUsers) {
            throw new IllegalStateException(
                    "Aucun utilisateur configuré et fastsearch.security.default-users=false");
        }
        return List.of(
                new User("admin", "admin-change-me", List.of("ADMIN"), List.of("A", "B")),
                new User("analyst", "analyst-change-me", List.of("ANALYST"), List.of("A")),
                new User("reader", "reader-change-me", List.of("READER"), List.of("A"))
        );
    }

    public static class User {
        private String username;
        private String password;
        private List<String> roles = new ArrayList<>();
        private List<String> groups = new ArrayList<>();

        public User() {
        }

        public User(String username, String password, List<String> roles, List<String> groups) {
            this.username = username;
            this.password = password;
            this.roles = roles == null ? new ArrayList<>() : new ArrayList<>(roles);
            this.groups = groups == null ? new ArrayList<>() : new ArrayList<>(groups);
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles == null ? new ArrayList<>() : roles;
        }

        public List<String> getGroups() {
            return groups;
        }

        public void setGroups(List<String> groups) {
            this.groups = groups == null ? new ArrayList<>() : groups;
        }
    }
}
