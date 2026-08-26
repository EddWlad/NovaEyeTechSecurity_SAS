# Verify Frontend Contract

Check that the API still honours the HTTP contract the Angular frontend in `../NextEyeSecurity/apps/frontend` expects.

## Why

This backend replaced a NestJS one without touching the frontend. Every shape it returns is consumed by code that was written against the old backend, so a response change is a breaking change even when the new shape is objectively nicer.

## Required Input

* A running instance: `docker compose up -d` and `JWT_SECRET=... SEED_ENABLED=true ./mvnw spring-boot:run`.
* Credentials of an `ADMINISTRADOR` and, ideally, of a `TECNICO`.

## Contract points

| Point | Expected |
|---|---|
| Prefix | Every route hangs off `/api` |
| Login | `POST /api/auth/login` returns `access_token` (snake_case) and `user` |
| Updates | `PATCH`, not `PUT` |
| Delete | Body `{ "message": "..." }` |
| Listing without `?page` | Plain array |
| Listing with `?page` | `{ page, limit, total, totalPages, hasNext, hasPrevious, items }` |
| Money | String with two decimals (`"117.30"`), never a JSON number |
| Errors | Body carries `message` as a String — the frontend interceptor reads `error.message` |
| 401 | Triggers logout in the frontend: it must only appear for a genuinely invalid or missing token |
| Quotation PDF | `GET /api/quotations/{id}/pdf`, `Content-Type: application/pdf`, `Content-Disposition: inline` |
| Evidence upload | Multipart with the file under the field name `file` |
| Relations | Come nested in the response (`client`, `category`, `technician`), not just as flat ids |

## Workflow

1. Read `.agents/features/pagination-dual-mode.md`.
2. Log in and keep the token.
3. For each affected resource, hit the listing with and without `?page` and confirm both shapes.
4. Confirm the money fields arrive as strings and the nested relations are present.
5. Force an error (unknown field, missing id, wrong role) and confirm the body carries `message`.
6. When quotations were touched, download the PDF and confirm the status, content type and header.
7. Cross-check against the frontend source when in doubt: `core/services/api.service.ts`, `core/interceptors/http-error.interceptor.ts`, `core/config/resource-definitions.ts` and the feature service involved.

## Acceptance Criteria

- Both pagination modes work on every listing touched.
- No monetary field is serialized as a JSON number.
- Every error response carries `message` as a String.
- Any deviation from the contract is reported explicitly, along with the frontend file that would break.
