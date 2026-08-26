package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.CategoryRequest;
import com.tidsec.novaeyetech_backend.model.ProductCategory;
import java.util.UUID;

public interface IProductCategoryService extends ICRUD<ProductCategory, UUID> {

    ProductCategory create(CategoryRequest request);

    ProductCategory update(UUID id, CategoryRequest request);
}
