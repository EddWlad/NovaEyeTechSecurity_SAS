package com.tidsec.novaeyetech_backend.dto.common;

import lombok.Builder;

/**
 * Comando de registro en bitacora.
 *
 * <p>Se declara como record con builder para que las llamadas queden legibles: siete parametros
 * sueltos en una firma serian intercambiables por error.
 *
 * @param payload objeto que se serializa a JSON como resumen del cuerpo de la operacion
 */
@Builder
public record AuditEntry(
        String module,
        String entity,
        String entityId,
        String action,
        String user,
        String summary,
        Object payload
) {

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_UPDATE_STATUS = "UPDATE_STATUS";
}
