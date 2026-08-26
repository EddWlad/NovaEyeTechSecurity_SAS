package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.UserRequest;
import com.tidsec.novaeyetech_backend.exception.DuplicateResourceException;
import com.tidsec.novaeyetech_backend.model.User;
import com.tidsec.novaeyetech_backend.repo.IGenericRepo;
import com.tidsec.novaeyetech_backend.repo.IUserRepo;
import com.tidsec.novaeyetech_backend.service.IStorageService;
import com.tidsec.novaeyetech_backend.service.IUserService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends CRUDImpl<User, UUID> implements IUserService {

    private static final String AVATAR_FOLDER = "avatars";

    private final IUserRepo repo;
    private final PasswordEncoder passwordEncoder;
    private final DtoMapper dtoMapper;
    private final IStorageService storageService;

    @Override
    protected IGenericRepo<User, UUID> getRepo() {
        return repo;
    }

    @Override
    protected String notFoundMessage() {
        return "Usuario no encontrado";
    }

    @Override
    protected String relatedRecordsMessage() {
        return "No se puede eliminar el usuario porque tiene registros relacionados.";
    }

    @Override
    @Transactional
    public User create(UserRequest request) {
        String email = normalizeEmail(request.getEmail());

        if (repo.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("El email ya esta registrado");
        }

        User user = dtoMapper.map(request, User.class);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setActive(request.getActive() == null || request.getActive());

        return repo.save(user);
    }

    @Override
    @Transactional
    public User update(UUID id, UserRequest request) {
        User user = findById(id);

        // El orden importa: la unicidad se valida contra el email todavia guardado, y el hash se
        // aplica despues del mapeo para que la contrasena en claro nunca quede persistida.
        validateEmailAvailability(user, request);
        dtoMapper.patch(request, user);
        applyEmailChange(user, request);
        applyPasswordChange(user, request);

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        return repo.save(user);
    }

    /**
     * El propio perfil nunca cambia de rol: aunque el cuerpo traiga el campo, se descarta antes de
     * mapear. Evita una escalada de privilegios desde la pantalla de perfil.
     */
    @Override
    @Transactional
    public User updateOwnProfile(UUID id, UserRequest request) {
        request.setRole(null);
        request.setActive(null);

        return update(id, request);
    }

    @Override
    @Transactional
    public User updateAvatar(UUID id, MultipartFile file) {
        User user = findById(id);
        String previousAvatar = user.getAvatarDataUrl();

        IStorageService.StoredFile stored = storageService.upload(file, AVATAR_FOLDER);
        user.setAvatarDataUrl(stored.url());

        User saved = repo.save(user);
        // El anterior se borra despues de guardar: si la subida o el guardado fallan, el usuario
        // conserva la foto que ya tenia.
        storageService.deleteByUrl(previousAvatar);

        return saved;
    }

    private void validateEmailAvailability(User user, UserRequest request) {
        if (request.getEmail() == null) {
            return;
        }

        String email = normalizeEmail(request.getEmail());

        if (!email.equals(user.getEmail()) && repo.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("El email ya esta registrado");
        }
    }

    private void applyEmailChange(User user, UserRequest request) {
        if (request.getEmail() != null) {
            user.setEmail(normalizeEmail(request.getEmail()));
        }
    }

    private void applyPasswordChange(User user, UserRequest request) {
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
