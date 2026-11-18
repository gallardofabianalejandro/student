package dev.fabian.gallardo.student.domain.exception;

public class SampleItemAlreadyExistsException extends RuntimeException {

    public SampleItemAlreadyExistsException(String name) {
        super("Ya existe un SampleItem con el nombre: " + name);
    }
}
