package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.CategoryRequest;
import com.tidsec.novaeyetech_backend.exception.DuplicateResourceException;
import com.tidsec.novaeyetech_backend.model.ProductCategory;
import com.tidsec.novaeyetech_backend.repo.IGenericRepo;
import com.tidsec.novaeyetech_backend.repo.IProductCategoryRepo;
import com.tidsec.novaeyetech_backend.service.IProductCategoryService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl extends CRUDImpl<ProductCategory, UUID>
        implements IProductCategoryService {

    private final IProductCategoryRepo repo;
    private final DtoMapper dtoMapper;

    @Override
    protected IGenericRepo<ProductCategory, UUID> getRepo() {
        return repo;
    }

    @Override
    protected List<String> searchFields() {
        return List.of("name", "description");
    }

    @Override
    protected String notFoundMessage() {
        return "Categoria de producto no encontrada";
    }

    @Override
    protected String relatedRecordsMessage() {
        return "No se puede eliminar la categoria porque esta relacionada con productos.";
    }

    @Override
    @Transactional
    public ProductCategory create(CategoryRequest request) {
        String name = request.getName().trim();

        if (repo.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("La categoria ya existe");
        }

        ProductCategory category = dtoMapper.map(request, ProductCategory.class);
        category.setName(name);
        category.setActive(request.getActive() == null || request.getActive());

        return repo.save(category);
    }

    @Override
    @Transactional
    public ProductCategory update(UUID id, CategoryRequest request) {
        ProductCategory category = findById(id);

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
