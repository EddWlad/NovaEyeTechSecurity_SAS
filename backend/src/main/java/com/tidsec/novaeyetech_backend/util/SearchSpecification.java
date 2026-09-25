package com.tidsec.novaeyetech_backend.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Busqueda de texto libre sobre varios campos de una entidad.
 *
 * <p>Coincide si algun campo contiene el termino, sin distinguir mayusculas ni tildes: "camara"
 * encuentra "CÁMARA". Un campo con punto ({@code "category.name"}) recorre la relacion con LEFT JOIN,
 * para no descartar registros cuya relacion opcional es nula (un producto sin proveedor, por ejemplo).
 */
public final class SearchSpecification {

    private static final char ESCAPE = '\\';

    // translate() de Postgres, sin extensiones: cada letra con tilde se cambia por la de la misma
    // posicion. Van tambien las mayusculas por si lower() no las pliega segun la collation.
    private static final String ACCENTED = "áàäâéèëêíìïîóòöôúùüûñÁÀÄÂÉÈËÊÍÌÏÎÓÒÖÔÚÙÜÛÑ";
    private static final String PLAIN = "aaaaeeeeiiiioooouuuunaaaaeeeeiiiioooouuuun";

    private SearchSpecification() {
    }

    /** Termino normalizado, o null si no hay nada que buscar. */
    public static String normalize(String search) {
        return search == null || search.isBlank() ? null : search.trim();
    }

    public static <T> Specification<T> containsAny(String term, List<String> fields) {
        String pattern = "%" + escapeLike(fold(term)) + "%";

        return (root, query, cb) -> cb.or(fields.stream()
                .map(field -> cb.like(fold(cb, path(root, field)), pattern, ESCAPE))
                .toArray(Predicate[]::new));
    }

    /** El termino en minusculas y sin tildes, igual que {@link #fold(CriteriaBuilder, Expression)} deja la columna. */
    static String fold(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
    }

    private static Expression<String> fold(CriteriaBuilder cb, Expression<String> column) {
        return cb.function("translate", String.class, cb.lower(column), cb.literal(ACCENTED), cb.literal(PLAIN));
    }

    private static Expression<String> path(From<?, ?> root, String field) {
        String[] parts = field.split("\\.");
        From<?, ?> from = root;
        for (int i = 0; i < parts.length - 1; i++) {
            from = from.join(parts[i], JoinType.LEFT);
        }
        return from.get(parts[parts.length - 1]);
    }

    /** Un "%" o "_" escrito por el usuario se busca literal, no como comodin. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
