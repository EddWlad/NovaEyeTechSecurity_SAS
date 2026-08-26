package com.tidsec.novaeyetech_backend.repo;

import com.tidsec.novaeyetech_backend.model.Identifiable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/** Repositorio base de todas las entidades del dominio. */
@NoRepositoryBean
public interface IGenericRepo<T extends Identifiable<ID>, ID> extends JpaRepository<T, ID> {
}
