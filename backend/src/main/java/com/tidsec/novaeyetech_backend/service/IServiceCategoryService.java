package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.CategoryRequest;
import com.tidsec.novaeyetech_backend.model.ServiceCategory;
import java.util.UUID;

public interface IServiceCategoryService extends ICRUD<ServiceCategory, UUID> {

    ServiceCategory create(CategoryRequest request);

    ServiceCategory update(UUID id, CategoryRequest request);
}
