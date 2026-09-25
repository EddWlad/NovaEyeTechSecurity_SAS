package com.tidsec.novaeyetech_backend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tidsec.novaeyetech_backend.dto.UserRequest;
import com.tidsec.novaeyetech_backend.exception.BusinessRuleException;
import com.tidsec.novaeyetech_backend.model.User;
import com.tidsec.novaeyetech_backend.repo.IUserRepo;
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
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Foto de perfil: vive en el almacenamiento externo y solo la fijan los endpoints de subida.
 *
 * <p>Fijan que la foto se pueda quitar (el PATCH no podia, porque el mapeo ignora los nulos) y que
 * una URL enviada en el cuerpo no acabe en la entidad, donde luego se borraria del almacenamiento.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final String OLD_URL =
            "https://res.cloudinary.com/cuenta/image/upload/v1/novaeyetech/avatars/vieja.jpg";
    private static final String NEW_URL =
            "https://res.cloudinary.com/cuenta/image/upload/v2/novaeyetech/avatars/nueva.jpg";

    @Mock
    private IUserRepo repo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private DtoMapper dtoMapper;
    @Mock
    private IStorageService storageService;

    private UserServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(repo, passwordEncoder, dtoMapper, storageService);
        user = User.builder().id(UUID.randomUUID()).email("t@test.com").avatarDataUrl(OLD_URL).build();
    }

    @Test
    @DisplayName("Editar el perfil con una foto base64 se rechaza")
    void updateRejectsInlineAvatar() {
        UserRequest request = new UserRequest();
        request.setAvatarDataUrl("data:image/jpeg;base64,AAAA");

        assertThatThrownBy(() -> service.update(user.getId(), request)).isInstanceOf(BusinessRuleException.class);

        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("Una URL de foto enviada en el cuerpo se ignora antes de mapear")
    void updateIgnoresAvatarUrlFromRequest() {
        UserRequest request = new UserRequest();
        request.setAvatarDataUrl("https://res.cloudinary.com/cuenta/image/upload/v1/novaeyetech/products/ajena.jpg");
        when(repo.findById(user.getId())).thenReturn(Optional.of(user));
        when(repo.save(user)).thenReturn(user);

        service.update(user.getId(), request);

        assertThat(request.getAvatarDataUrl()).isNull();
        verify(dtoMapper).patch(request, user);
        assertThat(user.getAvatarDataUrl()).isEqualTo(OLD_URL);
    }

    @Test
    @DisplayName("Reemplazar la foto sube la nueva, guarda y solo entonces borra la anterior")
    void updateAvatarDeletesPreviousAfterSaving() {
        MockMultipartFile file = new MockMultipartFile("file", "yo.jpg", "image/jpeg", new byte[] {1});
        when(repo.findById(user.getId())).thenReturn(Optional.of(user));
        when(storageService.upload(file, "avatars"))
                .thenReturn(new IStorageService.StoredFile(NEW_URL, "id", "image", "nueva", 1, "image/jpeg"));
        when(repo.save(user)).thenReturn(user);

        User saved = service.updateAvatar(user.getId(), file);

        assertThat(saved.getAvatarDataUrl()).isEqualTo(NEW_URL);
        InOrder order = inOrder(storageService, repo);
        order.verify(storageService).upload(file, "avatars");
        order.verify(repo).save(user);
        order.verify(storageService).deleteByUrl(OLD_URL);
    }

    @Test
    @DisplayName("Quitar la foto la deja en null y elimina el archivo del almacenamiento")
    void removeAvatarClearsAndDeletes() {
        when(repo.findById(user.getId())).thenReturn(Optional.of(user));
        when(repo.save(user)).thenReturn(user);

        User saved = service.removeAvatar(user.getId());

        assertThat(saved.getAvatarDataUrl()).isNull();
        verify(storageService).deleteByUrl(OLD_URL);
    }
}
