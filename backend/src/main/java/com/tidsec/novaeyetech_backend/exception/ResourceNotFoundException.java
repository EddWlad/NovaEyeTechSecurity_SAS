package com.tidsec.novaeyetech_backend.exception;

/** Recurso inexistente o fuera del alcance del usuario autenticado. Se traduce a 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
