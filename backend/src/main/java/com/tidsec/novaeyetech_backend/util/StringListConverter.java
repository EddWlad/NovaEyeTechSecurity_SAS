package com.tidsec.novaeyetech_backend.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;

/**
 * Persiste una lista de strings como CSV en una sola columna.
 * Replica el tipo {@code simple-array} de TypeORM usado por `quotation_settings`.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String SEPARATOR = ",";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        return attribute == null || attribute.isEmpty() ? "" : String.join(SEPARATOR, attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }

        return Arrays.stream(dbData.split(SEPARATOR))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }
}
