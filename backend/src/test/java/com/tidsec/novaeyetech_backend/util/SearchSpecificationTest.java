package com.tidsec.novaeyetech_backend.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SearchSpecificationTest {

    @SuppressWarnings("unchecked")
    private final Root<Object> root = mock(Root.class, RETURNS_DEEP_STUBS);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);

    @Test
    @DisplayName("Sin texto no hay busqueda; con texto se recortan los espacios")
    void normalize() {
        assertThat(SearchSpecification.normalize(null)).isNull();
        assertThat(SearchSpecification.normalize("   ")).isNull();
        assertThat(SearchSpecification.normalize("  domo ")).isEqualTo("domo");
    }

    @Test
    @DisplayName("El termino se compara en minusculas y sin tildes")
    void foldsCaseAndAccents() {
        assertThat(SearchSpecification.fold("CÁMARA Ñandú Pingüino")).isEqualTo("camara nandu pinguino");
    }

    @Test
    @DisplayName("Busca en todos los campos, sin tildes, y trata % y _ como texto literal")
    @SuppressWarnings("unchecked")
    void escapesWildcardsAndSearchesEveryField() {
        SearchSpecification.containsAny("CÁMARA 50%_x", List.of("name", "brand"))
                .toPredicate(root, query, cb);

        verify(cb, times(2)).like(any(Expression.class), eq("%camara 50\\%\\_x%"), eq('\\'));
        verify(cb, times(2)).function(eq("translate"), eq(String.class), any(Expression.class), any(Expression.class), any(Expression.class));
    }

    @Test
    @DisplayName("Un campo con punto recorre la relacion con LEFT JOIN y no descarta relaciones nulas")
    void dottedFieldUsesLeftJoin() {
        SearchSpecification.containsAny("sisegusa", List.of("name", "mainSupplier.businessName"))
                .toPredicate(root, query, cb);

        verify(root).join("mainSupplier", JoinType.LEFT);
        verify(root, never()).join("mainSupplier", JoinType.INNER);
    }
}
