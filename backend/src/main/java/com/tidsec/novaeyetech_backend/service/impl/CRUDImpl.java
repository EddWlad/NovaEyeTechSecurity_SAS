package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.exception.RelatedRecordsException;
import com.tidsec.novaeyetech_backend.exception.ResourceNotFoundException;
import com.tidsec.novaeyetech_backend.model.Identifiable;
import com.tidsec.novaeyetech_backend.repo.IGenericRepo;
import com.tidsec.novaeyetech_backend.service.ICRUD;
import com.tidsec.novaeyetech_backend.util.PaginationSupport;
import com.tidsec.novaeyetech_backend.util.SearchSpecification;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lectura y borrado genericos. Cada servicio concreto solo aporta su repositorio y los mensajes en
 * el idioma de su dominio; el alta y la actualizacion las declara el servicio con su request DTO.
 */
public abstract class CRUDImpl<T extends Identifiable<ID>, ID> implements ICRUD<T, ID> {

    protected abstract IGenericRepo<T, ID> getRepo();

    /** Mensaje que ve el usuario cuando el recurso no existe. */
    protected abstract String notFoundMessage();

    /** Mensaje cuando el borrado choca contra una clave foranea. */
    protected String relatedRecordsMessage() {
        return "No se puede eliminar el registro porque tiene registros relacionados.";
    }

    /**
     * Campos donde busca {@link #findAll(String, Pageable)}. Un punto recorre una relacion
     * ({@code "category.name"}). Sin campos, la busqueda se ignora.
     */
    protected List<String> searchFields() {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAll() {
        return getRepo().findAll(PaginationSupport.newestFirst());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable pageable) {
        return getRepo().findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(String search, Pageable pageable) {
        String term = SearchSpecification.normalize(search);
        if (term == null || searchFields().isEmpty()) {
            return findAll(pageable);
        }
        return getRepo().findAll(SearchSpecification.containsAny(term, searchFields()), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public T findById(ID id) {
        return getRepo().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(notFoundMessage()));
    }

    @Override
    @Transactional
    public void delete(ID id) {
        T entity = findById(id);

        try {
            getRepo().delete(entity);
            getRepo().flush();
        } catch (DataIntegrityViolationException ex) {
            throw new RelatedRecordsException(relatedRecordsMessage());
        }
    }
}
