# Known Gaps and Open Questions

This chapter collects differences, risks, and open questions found during
static analysis. It is intentionally not a list of proven runtime bugs; some
items require live or test verification.

## High

### Secrets and Concrete Service URLs in `application.properties`

The production resource file contains database credentials, API keys, client
secrets, and staging/production-like hostnames. These values should not be
spread as production defaults or documentation examples.

Recommendations:

- Rotate secrets if they are real.
- Move values to external secret/configuration mechanisms.
- Reduce the repository file to safe placeholders.

### `spring.jpa.hibernate.ddl-auto=create`

The main configuration visibly sets `ddl-auto=create`. This can recreate tables
and hide Flyway schema gaps.

Recommendations:

- Clarify the target value for production profiles.
- Establish Flyway as the leading schema source.
- Add tests for migrations and schema validation.

### Role-Management Rights for `User`

Flyway migrations appear to assign role-management permissions to `User`,
including `create_role`, `read_role_permission`, and `delete_role`.

Recommendations:

- Create a business role matrix.
- Check migrations and Keycloak roles against the target model.
- Add negative tests for non-administrative users.

## Medium

### Flyway Does Not Clearly Cover All Entities

No clear create migrations were found for `policy_tbl`,
`consumer_download_history`, `pcf_requests_tbl`, and `pcf_response_tbl` in the
visible Flyway history.

Recommendations:

- Run migrations against a fresh PostgreSQL database.
- Create a schema diff against JPA entities.
- Add missing create migrations.

### `PermissionEntity` Is Probably Mapped to the Wrong Table

`PermissionEntity` is mapped to `sde_role`, while Flyway creates
`sde_permission`.

Recommendations:

- Clarify whether `PermissionEntity` is used.
- Correct the mapping or remove the unused entity.

### OpenAPI and Controllers Drift Apart

Examples:

- OpenAPI contains `POST /upload`, but no matching controller was found.
- Controllers contain `GET /submodels/csvfile/{submodelName}` and
  `GET /cache/clear-pcfurl`, which are missing from OpenAPI.
- PCF `Edc-Bpn` is a header in the controller but is not documented
  consistently in OpenAPI.
- Pagination parameters differ for some endpoints.

Recommendations:

- Generate OpenAPI from code or systematically align code and OpenAPI.
- Add contract tests for important API groups.

### README/API Documentation Is Partially Outdated

The README contains duplicated API tables, wrong or outdated paths, and a wrong
link to `src/main/resources/sde-open-api.yml`; the actual path is
`modules/sde-core/src/main/resources/sde-open-api.yml`.

Recommendations:

- Reduce README to overview and operations guidance.
- Derive technical API documentation from OpenAPI.
- Remove outdated tables.

### Open CORS and Broad Actuator Exposure

CORS allows `*` for origins, methods, headers, and exposed headers. Actuator
exposure is broad.

Recommendations:

- Use profile-specific CORS configuration.
- Restrict Actuator exposure to required endpoints.
- Reduce health details in production.

### Public Cache Endpoints with Mutating Behavior

`/cache/**` is public according to `SecurityConfig`. The endpoints clear
internal caches and use GET.

Recommendations:

- Add authentication/permission checks to cache-clear endpoints.
- Move mutating operations to POST or DELETE.

### Consumer Decline Uses GET

`/contract-agreements/{negotiationId}/consumer/decline` is a mutating operation
over GET.

Recommendation:

- Change it to POST or DELETE and update OpenAPI/frontend accordingly.

### CSV Download May Write JSON

Static analysis found that the CSV branch in the consumer ZIP writer is
commented out. As a result, JSON files may be written into the ZIP even when
CSV is requested.

Recommendations:

- Clarify expected behavior.
- Add tests for `downloadDataAs=csv|json`.

## Low to Medium

### Java Versions Conflict

The root POM declares Java 17, while the Dockerfile uses Java 19 images.

Recommendation:

- Decide on the target Java version and align Dockerfile and CI.

### `PolicyHubController` RequestBody Import

The POST handler suspiciously imports
`io.swagger.v3.oas.annotations.parameters.RequestBody` instead of Spring
`@RequestBody`. This may cause binding problems or may be unused.

Recommendations:

- Add a controller test for `POST /policy-hub/policy-content`.
- Check and correct the import if necessary.

### `public` Path Name vs. Security

`GET /api/{submodel}/public/{uuid}` is named public, but it is not included in
the public URL list. Static analysis therefore indicates that the global
authentication rule still applies.

Recommendations:

- Clarify whether the endpoint should truly be public.
- Align name, `SecurityConfig`, and OpenAPI.

### Delete Cleanup Is Not Complete

Visible delete behavior:

- EDC contract definition, policies, and asset are deleted.
- DTR submodel descriptor is removed.
- local database data is marked as deleted.

Not visible:

- DTR shell deletion
- BPN Discovery cleanup
- Submodel Server cleanup

Recommendations:

- Define target cleanup behavior.
- Document idempotent cleanup tests and retry/failure handling.

## Documentation Follow-Up Tasks

- Refine diagrams with real class names.
- Automate OpenAPI/controller comparison.
- Verify the table model against a fresh PostgreSQL database.
- Maintain the role/permission matrix as a dedicated table.
- Document operating profiles for local, test, staging, and production.

