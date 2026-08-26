package com.tidsec.novaeyetech_backend.repo;

import com.tidsec.novaeyetech_backend.model.Product;
import java.util.UUID;

public interface IProductRepo extends IGenericRepo<Product, UUID> {

    boolean existsByInternalCode(String internalCode);
}
