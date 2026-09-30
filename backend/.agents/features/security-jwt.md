# Autenticación y autorización (JWT)

## Resumen

La API es **stateless**: no hay sesión de servidor. El cliente obtiene un token en `POST /api/auth/login` y lo envía en `Authorization: Bearer <token>` en cada petición.

## Piezas

| Archivo | Rol |
|---|---|
| `config/SecurityConfig.java` | Cadena de filtros, reglas de autorización, CORS, `PasswordEncoder` |
| `security/JwtService.java` | Emite y verifica el token |
| `security/JwtAuthenticationFilter.java` | Autentica cada petición |
| `security/AuthenticatedUser.java` | Principal: `id`, `email`, `role`, `fullName` |
| `security/SecurityErrorResponder.java` | Cuerpo JSON de los 401 y 403 |
| `security/JwtProperties.java` | `app.jwt.secret` y `app.jwt.expiration` |

## Decisiones

**Denegar por defecto.** La regla base es `anyRequest().authenticated()` y además cada handler declara su `@PreAuthorize`. Es una diferencia deliberada con el backend NestJS original: allí el `RolesGuard` dejaba pasar cualquier handler sin `@Roles`, así que olvidar el decorador convertía el endpoint en accesible para cualquier usuario autenticado. Aquí omitir la anotación nunca degrada a acceso público.

**Revalidación contra la base.** El filtro no confía solo en la firma: busca el usuario por el email del claim y exige `active = true`. Un usuario desactivado pierde acceso de inmediato aunque su token siga vigente. Replica `JwtStrategy.validate` del backend original.

**El filtro no es un bean.** Lo instancia `SecurityConfig`. Si se anota con `@Component`, Spring Boot lo registra también como filtro del contenedor y se ejecuta dos veces por petición. Además, anotar sus métodos con `@Transactional` lo envuelve en un proxy CGLIB y `GenericFilterBean.init` falla con `NullPointerException` sobre `logger`.

**El secreto es obligatorio.** `app.jwt.secret` no tiene valor por defecto y `JwtService` valida al arrancar que exista y tenga al menos 32 caracteres (HS256). Preferimos que la aplicación no arranque a que firme con una clave conocida.

**Contrato del token.** Los claims replican los del backend NestJS: `sub` (id del usuario), `email` y `role`. Vigencia por `JWT_EXPIRES_IN`, 8 horas por defecto.

**Contraseñas.** BCrypt con 10 rondas, igual que el backend original, de modo que los hashes existentes siguen validando.

**Fuga de información.** Usuario inexistente, usuario desactivado y contraseña incorrecta devuelven el mismo mensaje: distinguirlos permitiría enumerar cuentas. Por la misma razón un `TECNICO` que pide una cotización ajena recibe 404 y no 403.

**`UserDTO` no declara `password`.** El hash no puede salir por un descuido de saneado manual.

## Roles

Existen `ADMINISTRADOR`, `ADMIN_OPERATIVO` y `TECNICO` (`model/enums/Role.java`; ver su Javadoc). La authority es `ROLE_<nombre>`, de modo que `@PreAuthorize("hasRole('ADMINISTRADOR')")` funciona con la convención estándar de Spring.
