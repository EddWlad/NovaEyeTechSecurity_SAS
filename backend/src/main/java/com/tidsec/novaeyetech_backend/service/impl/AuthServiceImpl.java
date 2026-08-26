package com.tidsec.novaeyetech_backend.service.impl;

import com.tidsec.novaeyetech_backend.dto.LoginRequest;
import com.tidsec.novaeyetech_backend.dto.LoginResponse;
import com.tidsec.novaeyetech_backend.dto.UserDTO;
import com.tidsec.novaeyetech_backend.model.User;
import com.tidsec.novaeyetech_backend.repo.IUserRepo;
import com.tidsec.novaeyetech_backend.security.JwtService;
import com.tidsec.novaeyetech_backend.service.IAuthService;
import com.tidsec.novaeyetech_backend.util.DtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autenticacion por credenciales.
 *
 * <p>Usuario inexistente, usuario desactivado y contrasena incorrecta devuelven el mismo mensaje:
 * distinguirlos permitiria enumerar cuentas validas.
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final String INVALID_CREDENTIALS = "Credenciales invalidas";

    private final IUserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DtoMapper dtoMapper;

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepo.findByEmailIgnoreCase(request.email().trim().toLowerCase())
                .filter(User::isActive)
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS);
        }

        return new LoginResponse(jwtService.generateToken(user), dtoMapper.map(user, UserDTO.class));
    }
}
