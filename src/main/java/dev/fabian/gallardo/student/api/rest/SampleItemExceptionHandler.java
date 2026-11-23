package dev.fabian.gallardo.student.api.rest;

import dev.fabian.gallardo.student.domain.exception.SampleItemAlreadyExistsException;
import dev.fabian.gallardo.student.domain.exception.SampleItemNotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones de dominio del ejemplo a respuestas HTTP (RFC 9457) solo para SampleItemController. */
@RestControllerAdvice(assignableTypes = SampleItemController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SampleItemExceptionHandler {

    @ExceptionHandler(SampleItemNotFoundException.class)
    ProblemDetail notFound(SampleItemNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage(), "SAMPLE_ITEM_NOT_FOUND");
    }

    @ExceptionHandler(SampleItemAlreadyExistsException.class)
    ProblemDetail conflict(SampleItemAlreadyExistsException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), "SAMPLE_ITEM_ALREADY_EXISTS");
    }

    private static ProblemDetail problem(HttpStatus status, @Nullable String detail, String errorCode) {
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("errorCode", errorCode);
        return problem;
    }
}
