package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.common.AuditEntry;
import com.tidsec.novaeyetech_backend.model.AuditLog;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Bitacora de auditoria.
 *
 * <p>No extiende {@code ICRUD} a proposito: la bitacora se escribe y se lee, nunca se edita ni se
 * borra desde la aplicacion. Heredar el CRUD generico expondria un {@code delete} sobre el registro
 * que existe justamente para no poder alterarse.
 *
 * <p>Tampoco hay interceptor: cada operacion mutante registra su propio log, asi que toda operacion
 * nueva que cambie datos debe llamar a {@link #register(AuditEntry)} para mantener la trazabilidad.
 */
public interface IAuditLogService {

    void register(AuditEntry entry);

    List<AuditLog> findRecent(int limit);

    Page<AuditLog> findAll(Pageable pageable);
}
