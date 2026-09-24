package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.ProductRequest;
import com.tidsec.novaeyetech_backend.model.Product;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface IProductService extends ICRUD<Product, UUID> {

    Product create(ProductRequest request, AuthenticatedUser actor);

    Product update(UUID id, ProductRequest request, AuthenticatedUser actor);

    void delete(UUID id, AuthenticatedUser actor);

    /**
     * Sube la imagen del producto al almacenamiento y guarda su URL. Reemplaza y borra la anterior
     * solo despues de guardar la nueva: si la subida falla, el producto conserva la que tenia.
     */
    Product updateImage(UUID id, MultipartFile file, AuthenticatedUser actor);

    /** Quita la imagen del producto y la elimina del almacenamiento. */
    Product removeImage(UUID id, AuthenticatedUser actor);
}
