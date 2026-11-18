package dev.fabian.gallardo.student.domain.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("El usuario ya existe: " + username);
    }
}
