package com.tidsec.novaeyetech_backend.model.enums;

/** Roles del sistema. Solo existen estos dos: no agregar mas sin pedido explicito. */
public enum Role {
    ADMINISTRADOR,
    TECNICO;

    /** Authority que consume Spring Security (`hasRole` antepone el prefijo ROLE_). */
    public String authority() {
        return "ROLE_" + name();
    }
}
