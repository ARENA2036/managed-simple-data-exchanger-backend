# Runtime Flows

This chapter describes the most important runtime flows. The flows were
reconstructed from controllers, services, executors, and facilitators. External
systems were not validated live.

## Provider Upload and Registration

Entry points:

- `POST /api/{submodel}/upload`
- `POST /api/{submodel}/manualentry`

Flow:

1. The provider sends a CSV file plus `meta_data`, or a JSON body.
2. `SubmodelProcessController` creates or receives a `processId`.
3. CSV files are stored and validated against the submodel schema.
4. `onFlyPolicyManagement` derives the policy from a new, existing, or absent
   policy.
5. `ProcessReportUseCase` starts a process report.
6. Rows are processed in parallel.
7. The executor chain validates, maps, generates UUIDs, and enriches the data.
8. The Digital Twin Registry is queried for shells; shells and submodel
   descriptors are created or updated.
9. DTR access rules are set.
10. EDC asset, access policy, usage policy, and contract definition are created
    or updated.
11. BPN Discovery and the Submodel Server are populated.
12. Data is stored in the matching submodel table.
13. The process report is finished with success, failure, and updated counts.
14. For PCF uploads, already approved requests may be served automatically.

Diagram source:
[runtime-flow-provider.puml](../media/diagram/architecture/runtime-flow-provider.puml)

## Consumer Search and Download

Entry points:

- `GET /api/query-data-offers`
- `POST /api/offer-policy-details`
- `POST /api/subscribe-data-offers`
- `POST /api/subscribe-download-data-offers`
- `POST /api/subscribe-download-data-offers-async`
- `GET /api/download-data-offers`

Flow:

1. The consumer searches by `manufacturerPartId`, `bpnNumber`, and optionally
   `submodel`.
2. If no BPN is provided, BPN Discovery is queried for the part ID.
3. Portal/Partner Pool provides connector information.
4. The consumer EDC catalog is queried.
5. EDC contract negotiation and EDR are created or reused.
6. The remote Digital Twin Registry is read via EDC/DTR discovery.
7. Submodel descriptors provide dataplane endpoints.
8. Data is downloaded through the provider dataplane.
9. Download history is persisted.
10. Synchronous download writes a ZIP response.

Known issue: static analysis found that the CSV branch in the ZIP writer is
commented out. Even when CSV is requested, JSON files may be written into the
ZIP output. This is tracked in [Known Gaps](known-gaps.md).

Diagram source:
[runtime-flow-consumer.puml](../media/diagram/architecture/runtime-flow-consumer.puml)

## Delete/Cleanup

Entry point:

- `DELETE /api/{submodel}/delete/{processId}`

Flow:

1. The provider requests deletion for a reference process.
2. The backend creates a new delete `processId`.
3. Old records are read through the reference process.
4. A delete process report is started.
5. For each record, EDC contract definition, policies, and asset are deleted.
6. The DTR submodel descriptor is removed from the shell.
7. Local submodel data is marked as deleted.
8. The delete process report is completed.

Not visible as complete cleanup:

- DTR shell deletion
- BPN Discovery cleanup
- Submodel Server cleanup
- PCF EDC deletion, because PCF uses a static PCF Exchange asset

Diagram source:
[delete-cleanup-flow.puml](../media/diagram/architecture/delete-cleanup-flow.puml)

## PCF Exchange

Entry points:

- `POST /api/pcf/request/{productId}`
- `POST /api/pcf/request/nonexistdataoffer`
- `GET /api/pcf/request/{requestId}`
- `POST /api/pcf/actionsonrequest`
- `GET /api/pcf/{type}/requests`
- `GET /api/pcf/productIds/{productId}`
- `PUT /api/pcf/productIds/{productId}`

Flow:

1. The consumer requests PCF data for a product.
2. Existing offers use the normal consumer/EDC flow.
3. If no data offer exists, PCF Exchange endpoints are discovered.
4. The provider receives the PCF request through the EDC dataplane proxy.
5. The provider stores request status and checks local PCF data.
6. The provider can approve, reject, or return data.
7. Data is returned through `PUT /api/pcf/productIds/{productId}`.
8. PCF responses are persisted locally.
9. After a PCF upload, pending or approved requests may be served
   automatically.

Diagram source:
[pcf-exchange-flow.puml](../media/diagram/architecture/pcf-exchange-flow.puml)

## Startup Flow: Digital Twin Registry as EDC Asset

The existing diagram `docs/media/diagram/sequenzes/register_dtr.puml`
describes optional startup registration of the Digital Twin Registry itself as
an EDC asset. The backend creates the asset, access policy, usage policy, and
contract definition if they do not already exist.

