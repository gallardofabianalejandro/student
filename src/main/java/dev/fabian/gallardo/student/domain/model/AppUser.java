package dev.fabian.gallardo.student.domain.model;

import java.util.Set;

/** Usuario autenticable. La contraseña viaja siempre como hash, nunca en claro. */
public record AppUser(String id, String username, String passwordHash, Set<Role> roles) {

    public AppUser {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id es obligatorio");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username es obligatorio");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash es obligatorio");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
