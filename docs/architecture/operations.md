# Operations and DevOps

## Build

The project is a Maven multi-module build with a Spring Boot parent.

Important facts:

- Java version in the root POM: 17
- Spring Boot: 3.4.x
- main artifact: `modules/sde-core`
- tests: JUnit 5, Mockito, Spring Test, Spring Security Test, Testcontainers
- integration-test database: PostgreSQL Testcontainer

Typical local commands:

```bash
mvn -pl modules/sde-core test
mvn -pl modules/sde-core -am test
mvn test
mvn -pl modules/sde-core -am package
```

Testcontainers requires a running Docker daemon.

## Runtime

According to configuration:

- context path: `/api`
- default port: 8080
- database: PostgreSQL
- migrations: Flyway under `modules/sde-core/src/main/resources/flyway`
- security: OAuth2 resource server
- health: readiness/liveness via Actuator

The backend needs these external systems for real operation:

- Keycloak/OAuth2 issuer
- PostgreSQL
- Provider EDC
- Consumer EDC
- Digital Twin Registry
- Portal/Partner Pool
- BPN Discovery
- Policy Hub
- Submodel Server or SDE as submodel data source

## Dockerfile

File: `build/Dockerfile`

Observations:

- build stage uses `maven:3-eclipse-temurin-19-focal`
- runtime stage uses `eclipse-temurin:19.0.2_7-jdk-focal`
- project POM declares Java 17
- Maven build in the Dockerfile is commented out
- runtime image copies `modules/sde-core/target/*.jar` from the build stage
- container runs as non-root user `sdeuser`
- exposed port: 8080

Risks/uncertainties:

- Java 19 in the Dockerfile conflicts with Java 17 in the Maven project and
  many Spring Boot Java 17 deployment expectations.
- Because the Maven build is commented out, the JAR must exist before the Docker
  build or the build pipeline must provide the target directory to the Docker
  build stage.

## Configuration

The production resource `application.properties` contains many concrete values.
For operations, it should be treated as an example/default file, not as a source
of secrets.

Important property groups:

- `spring.datasource.*`
- `spring.flyway.*`
- `spring.security.oauth2.resourceserver.jwt.issuer-uri`
- `keycloak.clientid`
- `edc.*`
- `edc.consumer.*`
- `digital-twins.*`
- `submodel.datasource.*`
- `portal.backend.*`
- `partner.pool.*`
- `bpndiscovery.*`
- `discovery.*`
- `policy.hub.*`
- `management.*`
- `logging.*`
- `edr.*`

## Observability

Available mechanisms:

- Actuator health readiness/liveness
- extensive logging
- process reports in the database
- failure logs per process
- download history

Notable concerns:

- `management.endpoints.web.exposure.include=*` is broad.
- `management.endpoint.health.show-details=always` can expose sensitive details
  in production.
- root, HTTP, and Feign debug logging may leak payloads or technical details.

## Deployment Context

The README describes Auto-Setup as the central deployment orchestrator through
Helm charts. This repository contains the backend; according to the README, the
Helm chart lives in a separate `managed-simple-data-exchanger` repository.

Deployment documentation should separate:

- what this backend expects
- what Auto-Setup provides
- which properties and secrets are provided by operators
- which external services must already exist

## Operations Checklist

Clarify before production:

- Are all secrets externalized?
- Is `ddl-auto` set safely, for example `validate` or `none` instead of
  `create`?
- Are Flyway migrations complete for all tables?
- Is CORS restricted?
- Are Swagger/API docs intended to be public in the target environment?
- Are cache-clear endpoints protected?
- Do the Dockerfile and Maven project use the same Java version?
- Are health details and Actuator exposure appropriate?
- Are there smoke tests against EDC, DTR, Portal, BPN Discovery, and Policy Hub?

