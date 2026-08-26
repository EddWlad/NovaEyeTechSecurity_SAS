package com.tidsec.novaeyetech_backend.repo;

import com.tidsec.novaeyetech_backend.model.ProductCategory;
import java.util.UUID;

public interface IProductCategoryRepo extends IGenericRepo<ProductCategory, UUID> {

    boolean existsByNameIgnoreCase(String name);
}
