package com.tidsec.novaeyetech_backend.exception;

/** Violacion de unicidad detectada por la aplicacion (email, RUC, codigo interno). Se traduce a 400. */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
