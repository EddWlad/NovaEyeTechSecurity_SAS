# Generate Test Data

Generate JSON seed payloads and insert them via curl to populate CRUD entity data, in a dependency-safe sequence, excluding `User` and `AuditLog`.

## Assigned Agent

Use the `spring-test-data-generator` agent to produce the JSON payload files. This workflow defines the execution sequence and runs the curl calls — the agent does not execute requests itself.

## Required Input

* Base URL of the running application (defaults to `http://localhost:8080/api`).
* Credentials of an existing `ADMINISTRADOR`, used only at run time to obtain a bearer token. Never hardcode them into committed files.

## Population Sequence

Entities depend on each other in this order; later entities need ids produced by earlier ones:

1. `ProductCategory` — no dependencies.
2. `ServiceCategory` — no dependencies.
3. `Supplier` — no dependencies.
4. `Client` — no dependencies.
5. `Product` — needs `categoryId` from step 1 and optionally `mainSupplierId` from step 3.
6. `ServiceItem` — needs `categoryId` from step 2.
7. `Quotation` — needs `clientId` from step 4 and `productId`/`serviceId` from steps 5-6.
8. `Maintenance` — needs `clientId` from step 4; `technicianId` is optional and defaults to the authenticated user.
9. `MaintenanceComment` — needs `maintenanceId` from step 8.

## Workflow

1. Read `CLAUDE.md` and re-derive the in-scope entity list by checking which controllers expose a create (`POST`) endpoint, excluding `User` and `AuditLog`.
2. Obtain a token: `curl -X POST {baseUrl}/auth/login -H "Content-Type: application/json" -d '{"email":"...","password":"..."}'` and read `access_token` from the response.
3. Delegate to `spring-test-data-generator` to produce `testdata/<entity>.json` for each in-scope entity.
4. For each entity in the order above, `curl -X POST {baseUrl}/<entity-path> -H "Authorization: Bearer $TOKEN"`, substituting the placeholder ids with the ones returned by the previous steps.
5. Confirm each call returned a success status before moving to the next dependent entity; stop and report if one fails. A 400 with `Campo no permitido` means the payload carries a field the DTO does not declare.
6. Return a final summary including:

    * Entities populated and how many records were inserted for each
    * The resolved ids used to satisfy dependencies
    * Any entity skipped and why (missing create endpoint, out of scope, or a failed dependency)

## Acceptance Criteria

- `User` and `AuditLog` are never populated by this workflow.
- No real credentials are committed to `testdata/` files — credentials are supplied at run time only.
- Entities are only inserted after their dependencies exist.
- Monetary fields are sent as strings with two decimals; percentages belong to the allowed lists in `quotation_settings`.
- The final summary lists every id created, so the data can be traced or cleaned up later.

## Alternative

For a quick local dataset, `SEED_ENABLED=true` runs `DataSeeder` at startup: it creates an administrator, a technician, a client, a supplier, a product, a service and a demo quotation, and only acts when the users table is empty.
