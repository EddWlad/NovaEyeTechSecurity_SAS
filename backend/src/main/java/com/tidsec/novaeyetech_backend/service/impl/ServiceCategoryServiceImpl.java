package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.CategoryRequest;
import com.tidsec.novaeyetech_backend.exception.DuplicateResourceException;
import com.tidsec.novaeyetech_backend.model.ServiceCategory;
import com.tidsec.novaeyetech_backend.repo.IGenericRepo;
import com.tidsec.novaeyetech_backend.repo.IServiceCategoryRepo;
import com.tidsec.novaeyetech_backend.service.IServiceCategoryService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ServiceCategoryServiceImpl extends CRUDImpl<ServiceCategory, UUID>
        implements IServiceCategoryService {

    private final IServiceCategoryRepo repo;
    private final DtoMapper dtoMapper;

    @Override
    protected IGenericRepo<ServiceCategory, UUID> getRepo() {
        return repo;
    }

    @Override
    protected String notFoundMessage() {
        return "Categoria de servicio no encontrada";
    }

    @Override
    protected String relatedRecordsMessage() {
        return "No se puede eliminar la categoria porque esta relacionada con servicios.";
    }

    @Override
    @Transactional
    public ServiceCategory create(CategoryRequest request) {
        String name = request.getName().trim();

        if (repo.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("La categoria ya existe");
        }

        ServiceCategory category = dtoMapper.map(request, ServiceCategory.class);
        category.setName(name);
        category.setActive(request.getActive() == null || request.getActive());

        return repo.save(category);
    }

    @Override
    @Transactional
    public ServiceCategory update(UUID id, CategoryRequest request) {
        ServiceCategory category = findById(id);

        if (request.getName() != null) {
            String name = request.getName().trim();

            if (!name.equalsIgnoreCase(category.getName()) && repo.existsByNameIgnoreCase(name)) {
                throw new DuplicateResourceException("La categoria ya existe");
            }
            category.setName(name);
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        return repo.save(category);
    }
}
