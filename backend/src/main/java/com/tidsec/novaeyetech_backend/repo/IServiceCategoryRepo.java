package com.tidsec.novaeyetech_backend.repo;

import com.tidsec.novaeyetech_backend.model.ServiceCategory;
import java.util.UUID;

public interface IServiceCategoryRepo extends IGenericRepo<ServiceCategory, UUID> {

    boolean existsByNameIgnoreCase(String name);
}
