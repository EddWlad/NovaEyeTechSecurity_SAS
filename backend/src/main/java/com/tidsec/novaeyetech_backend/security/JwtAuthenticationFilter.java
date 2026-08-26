package com.tidsec.novaeyetech_backend.security;

import com.tidsec.novaeyetech_backend.model.User;
import com.tidsec.novaeyetech_backend.repo.IUserRepo;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica cada peticion a partir del Bearer token.
 *
 * <p>Igual que {@code JwtStrategy.validate} en NestJS, el usuario se revalida contra la base en cada
 * peticion: un usuario desactivado deja de tener acceso aunque su token siga vigente.
 *
 * <p>No se declara como bean de Spring: lo instancia {@code SecurityConfig} y lo inserta en la
 * cadena de seguridad. Registrarlo como bean haria que Boot lo anadiera tambien como filtro del
 * contenedor, ejecutandolo dos veces por peticion.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final IUserRepo userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            extractToken(request)
                    .flatMap(jwtService::parseToken)
                    .flatMap(this::resolveActiveUser)
                    .ifPresent(user -> authenticate(user, request));
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        return header != null && header.startsWith(BEARER_PREFIX)
                ? Optional.of(header.substring(BEARER_PREFIX.length()).trim())
                : Optional.empty();
    }

    private Optional<User> resolveActiveUser(Claims claims) {
        String email = claims.get("email", String.class);

        if (email == null) {
            return Optional.empty();
        }

        return userRepo.findByEmailIgnoreCase(email).filter(User::isActive);
    }

    private void authenticate(User user, HttpServletRequest request) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole());

        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(user.getRole().authority())));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
