package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.ProductRequest;
import com.tidsec.novaeyetech_backend.dto.common.AuditEntry;
import com.tidsec.novaeyetech_backend.exception.BusinessRuleException;
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
import com.tidsec.novaeyetech_backend.service.IStorageService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import com.tidsec.novaeyetech_backend.util.InlineImageGuard;
import com.tidsec.novaeyetech_backend.util.MoneyUtils;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends CRUDImpl<Product, UUID> implements IProductService {

    private static final String MODULE = "products";
    private static final String ENTITY = "Product";
    private static final String IMAGE_FOLDER = "products";

    private final IProductRepo repo;
    private final IProductCategoryRepo categoryRepo;
    private final ISupplierRepo supplierRepo;
    private final IAuditLogService auditLogService;
    private final IStorageService storageService;
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
        InlineImageGuard.reject(request.getImageUrl());
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
        InlineImageGuard.reject(request.getImageUrl());
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
        String imageUrl = product.getImageUrl();

        delete(id);
        // Solo si el borrado prospero: un producto con registros relacionados conserva su imagen.
        storageService.deleteByUrl(imageUrl);

        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(id.toString())
                .action(AuditEntry.ACTION_DELETE)
                .user(actor.email())
                .summary("Producto eliminado: " + name)
                .build());
    }

    @Override
    @Transactional
    public Product updateImage(UUID id, MultipartFile file, AuthenticatedUser actor) {
        requireImage(file);
        Product product = findById(id);
        String previousImage = product.getImageUrl();

        IStorageService.StoredFile stored = storageService.upload(file, IMAGE_FOLDER);
        product.setImageUrl(stored.url());

        Product saved = repo.save(product);
        // Una imagen heredada en base64 no es un recurso del proveedor: deleteByUrl la ignora y
        // simplemente queda sobrescrita.
        storageService.deleteByUrl(previousImage);
        registerImageAudit(saved, actor, "Imagen de producto actualizada: ");

        return saved;
    }

    @Override
    @Transactional
    public Product removeImage(UUID id, AuthenticatedUser actor) {
        Product product = findById(id);
        String previousImage = product.getImageUrl();

        product.setImageUrl(null);

        Product saved = repo.save(product);
        storageService.deleteByUrl(previousImage);
        registerImageAudit(saved, actor, "Imagen de producto eliminada: ");

        return saved;
    }

    /** El almacenamiento acepta tambien PDF y Office: para un producto solo tiene sentido una imagen. */
    private void requireImage(MultipartFile file) {
        String contentType = file == null ? null : file.getContentType();

        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BusinessRuleException("El archivo debe ser una imagen (PNG, JPG o WEBP)");
        }
    }

    private void registerImageAudit(Product product, AuthenticatedUser actor, String summaryPrefix) {
        auditLogService.register(AuditEntry.builder()
                .module(MODULE)
                .entity(ENTITY)
                .entityId(product.getId().toString())
                .action(AuditEntry.ACTION_UPDATE)
                .user(actor.email())
                .summary(summaryPrefix + product.getName())
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
