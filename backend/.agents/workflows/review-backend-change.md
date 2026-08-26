# Review Backend Change

Review backend code for correctness, risk, and adherence to project conventions.

## Assigned Agent

Use the `spring-api-reviewer` agent to perform the review. The agent invokes the `java-code-review` skill for the generic Java checklist (null safety, exception handling, concurrency, resource management); the workflow below covers the Spring/REST-specific checks the skill does not.

## Required Input
* Review target — one of:
    * A file or list of files (e.g. `ClientServiceImpl.java`).
    * A package (e.g. `com.tidsec.novaeyetech_backend.controller`).
    * An entity/vertical flow by name (e.g. "el flujo de cotizaciones"), meaning all of its layers: model, dto, repo, service, service.impl, controller.
    * A specific commit/branch range or the current working diff (`git diff`) , only when the user explicitly asks for a change/diff review instead of a code review.
* When the target has no pending changes (the common case for a named flow/file/package), this is a best-practices audit of the existing code, not a diff review.
* Whether changed resources are catalog-style and should be checked against the `Client` baseline (default: yes when they follow that shape).

## Workflow

1. Read `CLAUDE.md` to understand project conventions.
2. Resolve the review target to concrete files: for a named flow, gather all of its layers (model, dto, repo, service, service.impl, controller); for a package, list its files; for a diff/commit range, inspect `git diff --stat` and the relevant diffs instead.
3. Delegate the review to the `spring-api-reviewer` agent.
4. When the target is a catalog-style resource, check it against the `Client` baseline as a best-practices/conventions reference — i.e. does it follow the same patterns as `Client` and CLAUDE.md today — not as an old-version-to-new-version comparison. This applies whether or not the vertical has actually changed.
5. Confirm REST behavior, persistence, mapping, validation, security, and configuration were checked, and that missing tests were called out.
6. Run a targeted compile or test command when useful and feasible.
7. Review the agent's output for completeness; if findings lack file/line references or severity, or if findings describe a change instead of a convention deviation, ask it to revise before continuing.
8. Return a final summary including:

    * Findings, ordered by severity, with file/line references
    * Open questions or assumptions
    * Verification performed (compile/test commands run)
    * Residual test gaps

## Acceptance Criteria

- Findings include file and line references.
- The review prioritizes runtime and API-impacting issues.
- If no findings exist, the response states that clearly and identifies residual risk.