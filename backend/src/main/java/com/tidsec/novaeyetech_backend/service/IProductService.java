package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.ProductRequest;
import com.tidsec.novaeyetech_backend.model.Product;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import java.util.UUID;

public interface IProductService extends ICRUD<Product, UUID> {

    Product create(ProductRequest request, AuthenticatedUser actor);

    Product update(UUID id, ProductRequest request, AuthenticatedUser actor);

    void delete(UUID id, AuthenticatedUser actor);
}
