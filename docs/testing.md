# Testing Strategy

This document consolidates the current testing notes and test-expansion tasks
for the Managed Simple Data Exchanger backend. It describes the test strategy,
the current baseline, the priority areas for additional coverage, and the
commands developers should use when changing the backend.

## Purpose

The test suite is the safety net for refactoring and feature work. It should
make regressions visible in the areas that matter most:

- security and permission checks
- OpenAPI and controller contracts
- Flyway schema and persistence assumptions
- provider upload, manual-entry, delete, and cleanup workflows
- consumer search, subscription, download, and history workflows
- PCF request and response state transitions
- CSV, JSON, mapper, converter, and utility logic
- EDC and Digital Twin Registry facilitator behavior

The local test suite must not depend on live Tractus-X systems. EDC, Digital
Twin Registry, Portal, BPN Discovery, Policy Hub, and Submodel Server behavior
should be covered with local fakes, stubs, mocks against interfaces, or
Testcontainers only where a real infrastructure component is required.

## Strategy

Use the smallest test type that protects the relevant contract.

| Layer | Use for | Preferred approach |
| --- | --- | --- |
| Unit tests | Pure logic, mappers, converters, validators, JSON cleanup, status transitions | JUnit with local objects, fakes, or interface mocks |
| Static contract tests | OpenAPI/controller drift and permission seed drift | Parse source and resource files without starting Spring |
| Controller tests | Request binding, status codes, headers, default values, and delegation | Standalone MockMvc or focused Spring MVC tests |
| Security tests | Authentication, authorization, `@PreAuthorize`, and permission behavior | Spring Security test support with active security |
| Service workflow tests | Provider, consumer, delete, and PCF workflow decisions | Local fakes for external systems; avoid live services |
| Persistence tests | Flyway migrations, seed data, constraints, and repository behavior | Testcontainers PostgreSQL |

Tests should protect stable behavior, not incidental implementation details.
When a test documents a known current-state gap, the test name and documentation
should make that explicit so later changes can update the expected behavior
deliberately.

## Test Principles

- Keep tests deterministic and locally executable.
- Do not call live or shared external systems.
- Cover happy paths and negative paths for every critical workflow.
- Prefer hand-written fakes or interface mocks over Mockito-inline for concrete
  classes. The current Java 25 environment has shown Mockito agent warnings and
  older broad tests can be fragile.
- Use Testcontainers only for behavior that needs a real PostgreSQL database,
  such as Flyway, constraints, and repository integration.
- Give tests behavior-focused names.
- Add class-level Javadoc to new core workflow tests explaining what is tested,
  why it matters, and which regression risk it protects.
- Update this document when adding a new test category, workflow, or important
  command.

## Command Reference

Run the full core-module suite:

```bash
mvn -pl modules/sde-core -am test
```

Run the focused current-state and security regression suite:

```bash
mvn -pl modules/sde-core -Dtest='SecurityConfigAuthoritiesConverterTest,SecurityFilterChainRegressionTest,CustomPermissionEvaluatorTest,RoleManagementMethodSecurityRegressionTest,FlywaySchemaCurrentStateTest,OpenApiControllerDriftCurrentStateTest,ControllerPermissionSeedCoverageTest,PcfExchangeControllerRegressionTest' test
```

Run fast coverage-expansion tests that avoid live systems and Docker:

```bash
mvn -pl modules/sde-core -Dtest='ListToStringConverterTest,PoliciesListToStringConverterTest,CsvUtilTest,CsvHandlerServiceTest,SubmoduleUtilityTest,PingControllerRegressionTest,ProcessReportControllerRegressionTest,PolicyHubControllerRegressionTest,ConsumerControllerRegressionTest' test
```

Run focused core-logic tests for executor, database, process report, EDC, and
Digital Twin Registry behavior:

```bash
mvn -pl modules/sde-core -Dtest='GenericSubmodelExecutorCoreLogicTest,SubmoduleResponseHandlerCoreLogicTest,EDCUsecaseHandlerCoreLogicTest,DigitalTwinUseCaseHandlerCoreLogicTest,DigitalTwinLookUpInRegistryCoreLogicTest,DigitalTwinAccessRuleFacilatorCoreLogicTest,DatabaseUsecaseHandlerCoreLogicTest,ProcessReportUseCaseCoreLogicTest' test
```

Run focused EDC facilitator tests:

```bash
mvn -pl modules/sde-external-services/edc -Dtest='CreateEDCAssetFacilitatorCoreLogicTest,DeleteEDCFacilitatorCoreLogicTest' test
```

Run focused Digital Twin Registry facilitator tests:

```bash
mvn -pl modules/sde-external-services/digital-twins -Dtest='DigitalTwinsFacilitatorCoreLogicTest' test
```

Run focused PCF state-machine tests:

```bash
mvn -pl modules/pcf-exchange -Dtest='PCFRepositoryServiceCoreLogicTest' test
```

`FlywaySchemaCurrentStateTest` requires Docker because it starts PostgreSQL with
Testcontainers. If a broad Maven run fails because of pre-existing legacy tests,
record the failing command and the concrete failure reason.

## Current Baseline

The existing suite already covers several important current-state contracts:

- security role extraction from realm and client claims
- custom permission evaluation
- selected security filter-chain behavior
- method-security protection for role management
- Flyway migration smoke coverage with PostgreSQL Testcontainers
- OpenAPI/controller drift documentation
- controller permission seed coverage
- PCF controller request binding and metadata forwarding
- CSV handling, utility conversion, and submodule header extraction
- selected controller delegation behavior
- executor step ordering and response cleanup
- EDC create/update/delete branching
- database and process-report delegation
- Digital Twin Registry lookup, access rules, shell update, and delete behavior
- PCF repository state transitions

Some controller tests use standalone MockMvc or disable filters. Those tests are
useful for request binding and delegation, but they do not replace tests with
the real Spring Security filter chain.

## Priority Matrix

| Priority | Area | Primary risk | Expected coverage |
| --- | --- | --- | --- |
| 1 | Fast unit and static contract tests | Low-level refactoring breaks mappers, converters, utility behavior, controller mappings, or seeded permissions | Mapper, converter, CSV, utility, OpenAPI drift, and permission seed tests |
| 2 | Controller and security tests | Request binding, status codes, headers, and authorization drift without service changes | MockMvc tests, method-security tests, real filter-chain checks for critical endpoints |
| 3 | Service workflow tests | Provider, consumer, delete, and PCF business workflows regress below the controller layer | Service tests with faked external systems and explicit negative paths |
| 4 | Persistence and integration tests | Flyway, seed data, constraints, or repositories drift from runtime assumptions | Testcontainers PostgreSQL tests for schema, seeds, constraints, and central repositories |
| 5 | External facilitator tests | EDC and Digital Twin Registry calls use wrong IDs, order, or error handling | Local fake gateway tests for facilitator create, update, lookup, access-rule, and delete behavior |

## Core Workflow Coverage

### Provider Upload and Manual Entry

Provider tests should cover CSV and JSON input processing, identifier
generation, validation, digital twin lookup and creation, EDC asset and policy
handling, BPN registration, Submodel Server upload, and process-report updates.

Important negative paths include invalid CSV data, missing required fields,
unknown submodel metadata, external service failures, duplicate or invalid IDs,
and missing permissions.

### Consumer Search and Download

Consumer tests should cover data-offer search, BPN lookup, connector discovery,
contract negotiation, EDR reuse or resolution, dataplane download, ZIP/JSON/CSV
output, and download history.

Important negative paths include no offers found, failed negotiation, missing or
invalid EDR data, external download failures, unsupported output formats, empty
submodel data, and missing required request parameters.

### PCF Exchange

PCF tests should cover request creation, provider-side request persistence,
approval and rejection actions, response upload, notification behavior, status
transitions, `Edc-Bpn` header handling, and history queries.

Important negative paths include missing headers, invalid BPN values, unknown
request IDs, invalid status transitions, missing permissions, and failed
external push operations.

### Delete and Cleanup

Delete tests should cover local delete markers, EDC cleanup, Digital Twin
Registry cleanup, Submodel Server cleanup, process-report updates, and
idempotent behavior when remote resources have already been deleted.

Important negative paths include missing local data, failed external cleanup,
partial cleanup failures, and repeated delete requests.

### Facilitator and Executor Logic

Core facilitator and executor tests protect the provider pipeline below the
controller layer. They should verify execution order, ID propagation, response
mapping, JSON cleanup, gateway failure wrapping, shell lookup behavior, access
rule generation, shell update merging, and deletion order.

These tests should prefer local fakes and should not start Spring unless Spring
behavior is the subject of the test.

## Remaining Gaps

The following gaps should remain visible until they are deliberately closed:

- full MockMvc tests with the real `SecurityFilterChain` for critical
  controllers
- positive and negative endpoint authorization tests for `Admin`, `User`, and
  `Creator`
- service-level tests for upload, delete, consumer download, and PCF flows with
  all external systems mocked
- broader OpenAPI/controller path comparison
- CSV-versus-JSON download output tests
- cleanup behavior across EDC, Digital Twin Registry, BPN Discovery, and
  Submodel Server once the target behavior is clarified
- deep asynchronous workflow tests for `SubmodelOrchestartorService`,
  `ConsumerService`, `PcfExchangeServiceImpl`, and
  `AsyncPushPCFDataForApproveRequest`
- repository-level PostgreSQL tests for process report and role-permission
  repositories beyond the current Flyway smoke coverage

## Definition of Done

A backend change is sufficiently covered when:

1. The chosen test type matches the risk of the change.
2. Critical happy paths are covered.
3. Critical negative paths are covered.
4. External systems are replaced by local fakes, stubs, interface mocks, or
   documented Testcontainers infrastructure.
5. Targeted Maven commands have been run and recorded.
6. Any skipped broad test run is documented with the reason.
7. Remaining test gaps are explicit and prioritized.
8. This document is updated when new test categories or workflows are added.

## Maintenance

This file is the canonical testing document for the backend. Previous separate
test-task notes have been consolidated here and should not be linked from new
documentation.
