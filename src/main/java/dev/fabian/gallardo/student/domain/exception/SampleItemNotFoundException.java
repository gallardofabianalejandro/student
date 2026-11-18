package dev.fabian.gallardo.student.domain.exception;

import java.util.UUID;

/** Ejemplo de excepcion de dominio -- reemplazar por excepciones especificas del negocio real. */
public class SampleItemNotFoundException extends RuntimeException {

    public SampleItemNotFoundException(UUID id) {
        super("SampleItem no encontrado: " + id);
    }
}
