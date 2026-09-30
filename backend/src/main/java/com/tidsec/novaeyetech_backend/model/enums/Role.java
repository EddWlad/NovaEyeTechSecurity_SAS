package com.tidsec.novaeyetech_backend.model.enums;

/**
 * Roles del sistema. No agregar mas sin pedido explicito: cada uno es un valor del tipo nativo
 * users_role_enum y exige su migracion.
 *
 * <ul>
 *   <li>{@code ADMINISTRADOR}: super administrador, acceso total.</li>
 *   <li>{@code ADMIN_OPERATIVO}: administrador del dia a dia. Todo lo del super administrador menos
 *       usuarios, parametros de cotizacion y auditoria.</li>
 *   <li>{@code TECNICO}: solo sus cotizaciones y mantenimientos asignados.</li>
 * </ul>
 */
public enum Role {
    ADMINISTRADOR,
    ADMIN_OPERATIVO,
    TECNICO;

    /** Authority que consume Spring Security (`hasRole` antepone el prefijo ROLE_). */
    public String authority() {
        return "ROLE_" + name();
    }
}
