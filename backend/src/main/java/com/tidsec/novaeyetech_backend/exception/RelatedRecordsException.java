package com.tidsec.novaeyetech_backend.exception;

/** Borrado bloqueado por integridad referencial. Se traduce a 400 con mensaje de negocio. */
public class RelatedRecordsException extends RuntimeException {

    public RelatedRecordsException(String message) {
        super(message);
    }
}
