package com.tidsec.novaeyetech_backend.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Operaciones de lectura y borrado comunes a los servicios de dominio.
 *
 * <p>El alta y la actualizacion no viven aqui: cada dominio las declara con su propio request DTO,
 * porque necesita resolver relaciones, normalizar campos y registrar auditoria. Un {@code save(T)}
 * generico no aportaria nada que los servicios puedan usar.
 */
public interface ICRUD<T, ID> {

    List<T> findAll();

    Page<T> findAll(Pageable pageable);

    T findById(ID id);

    void delete(ID id);
}
