# Backend Current State: Managed Simple Data Exchanger

This dossier describes the statically analyzed current state of the Managed
Simple Data Exchanger backend. It is intended to help developers, architects,
QA, security, and DevOps understand what the backend does, which parts it
orchestrates, and where current behavior, intended behavior, and open questions
diverge.

## Reading Path

1. [Architecture Overview](architecture/overview.md)
2. [Module Map](architecture/module-map.md)
3. [Runtime Flows](architecture/runtime-flows.md)
4. [API Surface](architecture/api-surface.md)
5. [External Systems](architecture/external-systems.md)
6. [Data Model and Flyway](architecture/data-model.md)
7. [Security](architecture/security.md)
8. [Operations and DevOps](architecture/operations.md)
9. [Known Gaps](architecture/known-gaps.md)

## Analysis Basis

The documentation is based on static analysis of these sources:

- root `pom.xml` and module POMs
- `README.md`, `INSTALL.md`, and module READMEs
- `modules/sde-core/src/main/java`
- `modules/sde-core/src/main/resources/application.properties`
- `modules/sde-core/src/main/resources/sde-open-api.yml`
- `modules/sde-core/src/main/resources/flyway`
- `modules/sde-external-services`
- `modules/sde-submodules`
- `modules/pcf-exchange`
- `build/Dockerfile`
- existing PlantUML diagram under `docs/media/diagram/sequenzes`

Not verified:

- live behavior against EDC, Digital Twin Registry, Portal, BPN Discovery,
  Policy Hub, or Submodel Server
- Maven or integration tests
- Docker build
- deployment through Helm or Auto-Setup

## Summary

The backend is a Spring Boot and Maven multi-module system for the
Tractus-X/Catena-X Simple Data Exchanger. It accepts submodel data through CSV
or JSON, validates and transforms the data, creates or updates digital-twin
shells and submodel descriptors in a Digital Twin Registry, creates EDC assets,
policies, and contract definitions, and stores processing and history
information in PostgreSQL.

It also acts as a consumer: it searches for data offers through EDC Catalog,
Portal, and BPN Discovery, closes contract negotiations, retrieves data through
EDC Dataplane/EDR, and writes download history. Product Carbon Footprint has a
dedicated PCF exchange workflow with requests, provider actions, and PCF data
delivery.

## Main Capabilities

- submodel catalog and use-case self-description
- provider upload through CSV and JSON
- automatic or existing policy selection
- creation/update of digital twins and submodel descriptors
- creation/update of EDC assets, policies, and contract definitions
- local submodel history and process reports
- consumer search, subscribe, download, and download history
- contract agreement management
- policy management and Policy Hub proxying
- Portal and BPN Discovery proxies
- role- and permission-based access control through OAuth2/JWT
- PCF-specific data exchange

## Current State vs. Intended Behavior

The documentation distinguishes between:

- **Verified from code:** controllers, services, entities, Flyway, security,
  Dockerfile, and properties were read.
- **Taken from repository documentation:** README, INSTALL, and module READMEs
  describe target context and operating expectations.
- **Derived from architecture:** sequences and end-to-end flows were
  reconstructed from controller, service, and facilitator chains.
- **Open or inconsistent:** differences between README, OpenAPI, and code are
  documented in [Known Gaps](architecture/known-gaps.md).

## Diagrams

Diagram sources are located under `docs/media/diagram/architecture`:

- [System Context](media/diagram/architecture/context.mmd)
- [Module Dependencies](media/diagram/architecture/module-dependencies.mmd)
- [External Systems](media/diagram/architecture/external-systems.mmd)
- [Security Overview](media/diagram/architecture/security-overview.mmd)
- [Persistence Overview](media/diagram/architecture/persistence-overview.mmd)
- [Provider Upload/Register Flow](media/diagram/architecture/runtime-flow-provider.puml)
- [Consumer Download Flow](media/diagram/architecture/runtime-flow-consumer.puml)
- [Delete/Cleanup Flow](media/diagram/architecture/delete-cleanup-flow.puml)
- [PCF Exchange Flow](media/diagram/architecture/pcf-exchange-flow.puml)
