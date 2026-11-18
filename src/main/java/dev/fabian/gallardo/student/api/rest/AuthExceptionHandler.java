package dev.fabian.gallardo.student.api.rest;

import dev.fabian.gallardo.student.domain.exception.InvalidCredentialsException;
import dev.fabian.gallardo.student.domain.exception.UsernameAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingRequestCookieException;

@RestControllerAdvice(assignableTypes = {AuthController.class, RegistrationController.class})
public class AuthExceptionHandler {

    @ExceptionHandler({InvalidCredentialsException.class, MissingRequestCookieException.class})
    ProblemDetail unauthorized() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail forbidden() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Solicitud no permitida");
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    ProblemDetail conflict(UsernameAlreadyExistsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
