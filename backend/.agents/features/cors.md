# CORS Configuration

## Summary

CORS is configured globally so the Angular frontend, served from a different origin, can consume the API.

## Implementation

- **File:** `src/main/java/com/tidsec/novaeyetech_backend/config/SecurityConfig.java`
- **Type:** a `CorsConfigurationSource` bean wired into the Spring Security filter chain via `.cors(...)`

It is part of the security configuration, not a separate `WebMvcConfigurer`: with Spring Security in the chain, the CORS handling must run inside it so preflight requests are resolved before authentication.

- **Path pattern:** `/**`
- **Allowed origins:** `app.cors.allowed-origins`, read from `CORS_ALLOWED_ORIGINS` (default `http://localhost:4200`)
- **Allowed methods:** `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`
- **Allowed headers:** `*`
- **Exposed headers:** `Content-Disposition` — the frontend needs it to name the downloaded quotation PDF and the maintenance evidence files
- **Preflight:** `OPTIONS /**` is `permitAll()` in the authorization rules

## Notes

- The origin list is configuration, not code: adding a deployment environment means setting `CORS_ALLOWED_ORIGINS`, not editing the class.
- `setAllowedOriginPatterns` is used instead of `setAllowedOrigins` so a wildcard entry stays valid if credentials are ever enabled.
- The API authenticates with a `Authorization: Bearer` header, not cookies, so `allowCredentials` stays off.
