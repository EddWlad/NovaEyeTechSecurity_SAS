package com.tidsec.novaeyetech_backend.security;

import com.tidsec.novaeyetech_backend.model.enums.Role;
import java.util.UUID;

/**
 * Principal que viaja en el contexto de seguridad. Equivale al {@code req.user} del backend NestJS.
 */
public record AuthenticatedUser(UUID id, String email, Role role) {

    /** El alcance de cotizaciones y mantenimientos depende de esto, no de una comprobacion de rol suelta. */
    public boolean isTechnician() {
        return role == Role.TECNICO;
    }
}
