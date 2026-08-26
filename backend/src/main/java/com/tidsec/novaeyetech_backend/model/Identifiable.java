package com.tidsec.novaeyetech_backend.model;

/**
 * Contrato minimo que toda entidad del dominio expone.
 *
 * <p>Es el bound generico de {@link com.tidsec.novaeyetech_backend.repo.IGenericRepo} y permite que
 * el codigo comun lea el identificador sin reflexion ni casts.
 */
public interface Identifiable<ID> {

    ID getId();
}
