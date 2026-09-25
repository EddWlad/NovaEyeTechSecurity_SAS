package com.tidsec.novaeyetech_backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tidsec.novaeyetech_backend.dto.ProductRequest;
import com.tidsec.novaeyetech_backend.exception.BusinessRuleException;
import com.tidsec.novaeyetech_backend.model.Product;
import com.tidsec.novaeyetech_backend.model.enums.Role;
import com.tidsec.novaeyetech_backend.repo.IProductCategoryRepo;
import com.tidsec.novaeyetech_backend.repo.IProductRepo;
import com.tidsec.novaeyetech_backend.repo.ISupplierRepo;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IAuditLogService;
import com.tidsec.novaeyetech_backend.service.IStorageService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Imagenes de producto: viven en el almacenamiento externo, nunca como base64 en la base.
 *
 * <p>Fijan las reglas que evitan volver al problema original (17 MB de imagenes en cada listado) y
 * el orden seguro al reemplazar: el archivo anterior solo se elimina despues de guardar el nuevo.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private static final AuthenticatedUser ACTOR =
            new AuthenticatedUser(UUID.randomUUID(), "admin@test.com", Role.ADMINISTRADOR);
    private static final String OLD_URL =
            "https://res.cloudinary.com/cuenta/image/upload/v1/novaeyetech/products/viejo.jpg";
    private static final String NEW_URL =
            "https://res.cloudinary.com/cuenta/image/upload/v2/novaeyetech/products/nuevo.jpg";

    @Mock
    private IProductRepo repo;
    @Mock
    private IProductCategoryRepo categoryRepo;
    @Mock
    private ISupplierRepo supplierRepo;
    @Mock
    private IAuditLogService auditLogService;
    @Mock
    private IStorageService storageService;
    @Mock
    private DtoMapper dtoMapper;

    private ProductServiceImpl service;
    private Product product;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(repo, categoryRepo, supplierRepo, auditLogService, storageService, dtoMapper);
        product = Product.builder().id(UUID.randomUUID()).name("Camara").imageUrl(OLD_URL).build();
    }

    @Test
    @DisplayName("Crear con una imagen base64 se rechaza y no toca la base")
    void createRejectsInlineImage() {
        ProductRequest request = new ProductRequest();
        request.setImageUrl("data:image/jpeg;base64,AAAA");

        assertThatThrownBy(() -> service.create(request, ACTOR)).isInstanceOf(BusinessRuleException.class);

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Editar con una imagen base64 se rechaza aunque el prefijo venga en mayusculas")
    void updateRejectsInlineImage() {
        ProductRequest request = new ProductRequest();
        request.setImageUrl("DATA:image/png;base64,AAAA");

        assertThatThrownBy(() -> service.update(product.getId(), request, ACTOR))
                .isInstanceOf(BusinessRuleException.class);

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Una URL enviada en el cuerpo se ignora: la imagen solo la fija el endpoint de subida")
    void updateIgnoresImageUrlFromRequest() {
        ProductRequest request = new ProductRequest();
        request.setImageUrl("https://res.cloudinary.com/cuenta/image/upload/v1/novaeyetech/avatars/ajena.jpg");
        when(repo.findById(product.getId())).thenReturn(Optional.of(product));
        when(repo.save(product)).thenReturn(product);

        service.update(product.getId(), request, ACTOR);

        assertThat(request.getImageUrl()).isNull();
        verify(dtoMapper).patch(request, product);
        assertThat(product.getImageUrl()).isEqualTo(OLD_URL);
    }

    @Test
    @DisplayName("Reemplazar la imagen sube la nueva, guarda y solo entonces borra la anterior")
    void updateImageDeletesPreviousAfterSaving() {
        MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[] {1, 2});
        when(repo.findById(product.getId())).thenReturn(Optional.of(product));
        when(storageService.upload(file, "products"))
                .thenReturn(new IStorageService.StoredFile(NEW_URL, "id", "image", "nuevo", 2, "image/jpeg"));
        when(repo.save(product)).thenReturn(product);

        Product saved = service.updateImage(product.getId(), file, ACTOR);

        assertThat(saved.getImageUrl()).isEqualTo(NEW_URL);
        InOrder order = inOrder(storageService, repo);
        order.verify(storageService).upload(file, "products");
        order.verify(repo).save(product);
        order.verify(storageService).deleteByUrl(OLD_URL);
    }

    @Test
    @DisplayName("Un archivo que no es imagen se rechaza antes de tocar el almacenamiento")
    void updateImageRejectsNonImage() {
        MockMultipartFile pdf = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[] {1});

        assertThatThrownBy(() -> service.updateImage(product.getId(), pdf, ACTOR))
                .isInstanceOf(BusinessRuleException.class);

        verify(storageService, never()).upload(any(), anyString());
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Quitar la imagen la deja en null y elimina el archivo del almacenamiento")
    void removeImageClearsAndDeletes() {
        when(repo.findById(product.getId())).thenReturn(Optional.of(product));
        when(repo.save(product)).thenReturn(product);

        Product saved = service.removeImage(product.getId(), ACTOR);

        assertThat(saved.getImageUrl()).isNull();
        verify(storageService).deleteByUrl(OLD_URL);
    }

    @Test
    @DisplayName("Eliminar el producto elimina tambien su imagen del almacenamiento")
    void deleteRemovesStoredImage() {
        when(repo.findById(product.getId())).thenReturn(Optional.of(product));

        service.delete(product.getId(), ACTOR);

        verify(repo).delete(product);
        verify(storageService).deleteByUrl(OLD_URL);
    }
}
