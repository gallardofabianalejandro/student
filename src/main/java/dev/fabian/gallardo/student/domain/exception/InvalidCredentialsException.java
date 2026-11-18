package dev.fabian.gallardo.student.domain.exception;

/** Credenciales o refresh token inválidos. El mensaje no distingue usuario inexistente de contraseña errónea. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Credenciales inválidas");
    }
}
