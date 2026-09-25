package com.tidsec.novaeyetech_backend.controller;

import com.tidsec.novaeyetech_backend.dto.UserDTO;
import com.tidsec.novaeyetech_backend.dto.UserRequest;
import com.tidsec.novaeyetech_backend.dto.common.MessageResponse;
import com.tidsec.novaeyetech_backend.dto.common.PaginationQuery;
import com.tidsec.novaeyetech_backend.dto.validation.OnCreate;
import com.tidsec.novaeyetech_backend.security.AuthenticatedUser;
import com.tidsec.novaeyetech_backend.service.IUserService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import com.tidsec.novaeyetech_backend.util.ListingResponder;
import com.tidsec.novaeyetech_backend.util.PaginationSupport;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Usuarios. La respuesta usa {@code UserDTO}, que no declara el campo password: el hash nunca sale
 * de la aplicacion, sin depender de un paso de saneado manual.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService service;
    private final ListingResponder listingResponder;
    private final DtoMapper dtoMapper;

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserDTO> create(@Validated(OnCreate.class) @RequestBody UserRequest request) {
        return ResponseEntity.ok(dtoMapper.map(service.create(request), UserDTO.class));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<Object> findAll(@Valid PaginationQuery query) {
        return listingResponder.respondWithSearch(query, PaginationSupport.DEFAULT_LIMIT,
                service::findAll, service::findAll, UserDTO.class);
    }

    /** Debe declararse antes que /{id} para que "me" no se interprete como identificador. */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<UserDTO> me(@AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(dtoMapper.map(service.findById(actor.id()), UserDTO.class));
    }

    @PatchMapping("/me/profile")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<UserDTO> updateOwnProfile(@AuthenticationPrincipal AuthenticatedUser actor,
                                                    @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(dtoMapper.map(service.updateOwnProfile(actor.id(), request), UserDTO.class));
    }

    /** Sube la foto de perfil a Cloudinary y deja su URL en el usuario. */
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<UserDTO> updateOwnAvatar(@AuthenticationPrincipal AuthenticatedUser actor,
                                                   @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(dtoMapper.map(service.updateAvatar(actor.id(), file), UserDTO.class));
    }

    /** Quita la foto de perfil y la elimina de Cloudinary. */
    @DeleteMapping("/me/avatar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR', 'TECNICO')")
    public ResponseEntity<UserDTO> removeOwnAvatar(@AuthenticationPrincipal AuthenticatedUser actor) {
        return ResponseEntity.ok(dtoMapper.map(service.removeAvatar(actor.id()), UserDTO.class));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(dtoMapper.map(service.findById(id), UserDTO.class));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<UserDTO> update(@PathVariable UUID id, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(dtoMapper.map(service.update(id, request), UserDTO.class));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<MessageResponse> delete(@PathVariable UUID id) {
        service.delete(id);

        return ResponseEntity.ok(new MessageResponse("Usuario eliminado correctamente"));
    }
}
