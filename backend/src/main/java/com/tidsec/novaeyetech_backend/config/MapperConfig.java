package com.tidsec.novaeyetech_backend.config;

import com.tidsec.novaeyetech_backend.dto.ProductDTO;
import com.tidsec.novaeyetech_backend.dto.ServiceDTO;
import com.tidsec.novaeyetech_backend.model.Product;
import com.tidsec.novaeyetech_backend.model.ServiceItem;
import org.modelmapper.Conditions;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de ModelMapper.
 *
 * <p>Dos decisiones sostienen el resto del codigo:
 * <ul>
 *   <li><b>STRICT</b>: solo mapea nombres que coinciden exactamente, para que un renombrado se note
 *       como campo vacio y no como campo mal poblado.</li>
 *   <li><b>skip nulls</b>: un origen null no pisa el destino, que es justo la semantica de PATCH.
 *       Asi un mismo request DTO sirve para crear y para actualizar parcialmente.</li>
 * </ul>
 */
@Configuration
public class MapperConfig {

    @Bean
    public ModelMapper defaultMapper() {
        ModelMapper mapper = new ModelMapper();

        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setPropertyCondition(Conditions.isNotNull())
                .setSkipNullEnabled(true);

        // El id de la relacion viaja plano en el request y anidado en la respuesta:
        // sin estos mapeos explicitos STRICT no encuentra la ruta profunda.
        mapper.createTypeMap(Product.class, ProductDTO.class)
                .addMapping(src -> src.getCategory().getId(), ProductDTO::setCategoryId)
                .addMapping(src -> src.getMainSupplier().getId(), ProductDTO::setMainSupplierId);

        mapper.createTypeMap(ServiceItem.class, ServiceDTO.class)
                .addMapping(src -> src.getCategory().getId(), ServiceDTO::setCategoryId);

        return mapper;
    }
}
