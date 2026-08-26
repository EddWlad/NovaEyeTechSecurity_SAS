# Paginación de doble modo

## Resumen

Casi todos los listados responden de dos formas distintas según la query string:

- **Sin `?page`** → array plano: `[{...}, {...}]`
- **Con `?page`** → objeto paginado:

```json
{ "page": 1, "limit": 10, "total": 42, "totalPages": 5, "hasNext": true, "hasPrevious": false, "items": [] }
```

## Por qué existe

El frontend Angular consume las dos formas desde sitios distintos: `ApiService.list()` alimenta selects y lookups de los formularios, `ApiService.listPaginated()` alimenta las tablas. **Romper el modo array rompe los combos de los formularios**, aunque las tablas sigan funcionando; romper el modo paginado rompe las tablas. Los dos modos son contrato público.

## Piezas

| Archivo | Rol |
|---|---|
| `dto/common/PaginationQuery.java` | Query string. `isPaginated()` es verdadero **solo si llega `page`** |
| `dto/common/PageResponse.java` | Forma de la respuesta paginada. Los nombres de campo son contrato |
| `util/PaginationSupport.java` | Traduce a `Pageable`, acota `limit` y fija el orden |
| `util/ListingResponder.java` | Resuelve el doble modo en un solo lugar |

## Reglas

- `limit` se acota a **1..100**.
- El default es **10**, salvo auditoría que usa **20**.
- El orden por defecto es `createdAt DESC`.
- `page` es 1-based en la API y se traduce a 0-based para Spring Data.

## Cómo se usa

Un listado normal delega en `ListingResponder`:

```java
return listingResponder.respond(query, PaginationSupport.DEFAULT_LIMIT,
        service::findAll, service::findAll, ClientDTO.class);
```

Cotizaciones, mantenimientos y adjuntos resuelven el doble modo dentro del controlador porque su consulta depende del usuario autenticado o de parámetros de ruta. El contrato de salida es idéntico: si se cambia uno hay que cambiar los demás.
