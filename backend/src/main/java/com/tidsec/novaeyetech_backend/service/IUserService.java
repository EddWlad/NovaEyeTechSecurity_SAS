package com.tidsec.novaeyetech_backend.service;

import com.tidsec.novaeyetech_backend.dto.UserRequest;
import com.tidsec.novaeyetech_backend.model.User;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface IUserService extends ICRUD<User, UUID> {

    User create(UserRequest request);

    User update(UUID id, UserRequest request);

    /** Actualiza el propio perfil: nunca permite cambiar el rol. */
    User updateOwnProfile(UUID id, UserRequest request);

    /**
     * Sube la foto de perfil al almacenamiento y guarda su URL.
     *
     * <p>El campo sigue llamandose {@code avatarDataUrl} por compatibilidad con el frontend, que lo
     * pone directo en el {@code src} de una imagen: un data URL heredado y una URL https funcionan
     * igual ahi.
     */
    User updateAvatar(UUID id, MultipartFile file);
}
