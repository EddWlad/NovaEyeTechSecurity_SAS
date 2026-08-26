package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.ProductRequest;
import com.tidsec.novaeyetech_backend.dto.common.AuditEntry;
import com.tidsec.novaeyetech_backend.exception.DuplicateResourceException;
import com.tidsec.novaeyetech_backend.exception.ResourceNotFoundException;
import com.tidsec.novaeyetech_backend.model.Product;
import com.tidsec.novaeyetech_backend.model.ProductCategory;
import com.tidsec.novaeyetech_backend.model.Supplier;
import com.tidsec.novaeyetech_backend.repo.IGenericRepo;
import com.tidsec.novaeyetech_backend.repo.IProductCategoryRepo;
import com.tidsec.novaeyetech_backend.repo.IProductRepo;
import com.tidsec.novaeyetech_backend.repo.ISupplierRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IAuditLogService;
import com.tidsec.novaeyetech_backend.service.IProductService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import com.tidsec.novaeyetech_backend.util.MoneyUtils;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends CRUDImpl<Product, UUID> implements IProductService {

    private static final String MODULE = "products";
    private static final String ENTITY = "Product";

    private final IProductRepo repo;
    private final IProductCategoryRepo categoryRepo;
    private final ISupplierRepo supplierRepo;
    private final IAuditLogService auditLogService;
    private final DtoMapper dtoMapper;

    @Override
    protected IGenericRepo<Product, UUID> getRepo() {
        return repo;
    }

    @Override
    protected String notFoundMessage() {
        return "Producto no encontrado";
    }

    @Override
    protected String relatedRecordsMessage() {
        return "No se puede eliminar el producto porque esta relacionado con otros registros.";
    }

    @Override
    @Transactional
    public Product create(ProductRequest request, AuthenticatedUser actor) {
        String internalCode = request.getInternalCode().trim();

        if (repo.existsByInternalCode(internalCode)) {
            throw new DuplicateResourceException("El codigo interno ya existe");
        }

        Product product = dtoMapper.map(request, Product.class);
        product.setInternalCode(internalCode);
        product.setCategory(resolveCategory(request.getCategoryId()));
        product.setMainSupplier(resolveSupplier(request.getMainSupplierId()));
        product.setBaseCost(MoneyUtils.scale(request.getBaseCost()));
        product.setStock(MoneyUtils.scale(request.getStock()));
        product.setActive(request.getActive() == null || request.getActive());

        Product saved = repo.save(product);
        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(saved.getId().toString())
                .action(AuditEntry.ACTION_CREATE)
                .user(actor.email())
                .summary("Producto creado: " + saved.getName())
                .payload(request)
                .build());

        return saved;
    }

    @Override
    @Transactional
    public Product update(UUID id, ProductRequest request, AuthenticatedUser actor) {
        Product product = findById(id);

        if (request.getInternalCode() != null) {
            String internalCode = request.getInternalCode().trim();

            if (!internalCode.equals(product.getInternalCode()) && repo.existsByInternalCode(internalCode)) {
                throw new DuplicateResourceException("El codigo interno ya existe");
            }
            product.setInternalCode(internalCode);
        }

        dtoMapper.patch(request, product);

        if (request.getCategoryId() != null) {
            product.setCategory(resolveCategory(request.getCategoryId()));
        }
        if (request.getMainSupplierId() != null) {
            product.setMainSupplier(resolveSupplier(request.getMainSupplierId()));
        }
        if (request.getBaseCost() != null) {
            product.setBaseCost(MoneyUtils.scale(request.getBaseCost()));
        }
        if (request.getStock() != null) {
            product.setStock(MoneyUtils.scale(request.getStock()));
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product saved = repo.save(product);
        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(saved.getId().toString())
                .action(AuditEntry.ACTION_UPDATE)
                .user(actor.email())
                .summary("Producto actualizado: " + saved.getName())
                .payload(request)
                .build());

        return saved;
    }

    @Override
    @Transactional
    public void delete(UUID id, AuthenticatedUser actor) {
        Product product = findById(id);
        String name = product.getName();

        delete(id);

        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(id.toString())
                .action(AuditEntry.ACTION_DELETE)
                .user(actor.email())
                .summary("Producto eliminado: " + name)
                .build());
    }

    private ProductCategory resolveCategory(UUID categoryId) {
        return categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria de producto no encontrada"));
    }

    private Supplier resolveSupplier(UUID supplierId) {
        if (supplierId == null) {
            return null;
        }

        return supplierRepo.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
    }
}
