package com.tidsec.novaeyetech_backend.util;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

/**
 * Fachada generica sobre ModelMapper.
 *
 * <p>Concentra la conversion entidad/DTO en un unico punto: los controladores no repiten
 * {@code mapper.map(...)} ni conocen la libreria de mapeo, de modo que cambiarla no obliga a tocar
 * cada vertical.
 */
@Component
@RequiredArgsConstructor
public class DtoMapper {

    private final ModelMapper defaultMapper;

    public <D> D map(Object source, Class<D> targetType) {
        return source == null ? null : defaultMapper.map(source, targetType);
    }

    public <D> List<D> mapList(List<?> source, Class<D> targetType) {
        return source.stream().map(element -> map(element, targetType)).toList();
    }

    /**
     * Vuelca el origen sobre una instancia ya existente. Los campos nulos del origen se ignoran
     * (ver {@code MapperConfig}), que es exactamente la semantica de una actualizacion parcial.
     */
    public void patch(Object source, Object target) {
        defaultMapper.map(source, target);
    }
}
