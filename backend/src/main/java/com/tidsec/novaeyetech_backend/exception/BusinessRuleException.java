package com.tidsec.novaeyetech_backend.exception;

/** Regla de negocio violada (descuento mayor al total, IVA no permitido, etc.). Se traduce a 400. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
