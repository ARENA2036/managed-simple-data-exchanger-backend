# API Surface

The REST API runs below `server.servlet.context-path=/api`. The static OpenAPI
file is `modules/sde-core/src/main/resources/sde-open-api.yml`. This chapter is
based primarily on controller classes and marks known differences from the
OpenAPI file.

## Submodel Catalog

Controller: `SubmodelController`

| Method | Path | Purpose | Auth |
| --- | --- | --- | --- |
| GET | `/api/submodels` | List supported submodels, optionally filtered by use cases | authenticated |
| GET | `/api/submodels/schema-details` | Full submodel schemas | authenticated |
| GET | `/api/submodels/{submodelName}` | Details for one submodel | authenticated |
| GET | `/api/usecases` | Use-case list from `use-case.json` | authenticated |

These endpoints do not have method-level `@PreAuthorize`, but the global
security rule still requires authentication for everything except public URLs.

## Provider Contract-Offer Lifecycle

Controllers: `SubmodelProcessController`, `SubmodelCsvController`

| Method | Path | Purpose | Permission |
| --- | --- | --- | --- |
| POST | `/api/{submodel}/upload` | CSV upload plus policy metadata | `provider_create_contract_offer` or `provider_update_contract_offer` |
| POST | `/api/{submodel}/manualentry` | JSON/manual entry | `provider_create_contract_offer` or `provider_update_contract_offer` |
| GET | `/api/{submodel}/public/{uuid}` | Access created twin/submodel data | no method annotation |
| DELETE | `/api/{submodel}/delete/{processId}` | EDC/DTR/database cleanup for a process | `provider_delete_contract_offer` |
| GET | `/api/submodels/csvfile/{submodelName}` | CSV template for a submodel | authenticated |
| GET | `/api/{submodel}/download/{processId}/csv` | Provider-owned data as CSV | `provider_download_own_data` |

Upload and delete usually return:

```json
{
  "processId": "..."
}
```

## Process Reports

Controller: `ProcessReportController`

| Method | Path | Purpose | Permission |
| --- | --- | --- | --- |
| GET | `/api/processing-report` | Paginated process reports | `provider_view_history` |
| GET | `/api/processing-report/{id}` | Process report details | `provider_view_history` |
| GET | `/api/processing-report/failure-details/{id}` | Failure details | `provider_view_history` |
| GET | `/api/processing-report/{submodel}/success-details/{id}` | Success details | `provider_view_history` |

## Consumer Data Offers and Download

Controller: `ConsumerController`

| Method | Path | Purpose | Permission |
| --- | --- | --- | --- |
| GET | `/api/query-data-offers` | Search data offers | `consumer_view_contract_offers` |
| POST | `/api/offer-policy-details` | Load policy details for offers | `consumer_view_contract_offers` |
| POST | `/api/subscribe-data-offers` | Establish a contract agreement | `consumer_establish_contract_agreement` |
| POST | `/api/subscribe-download-data-offers-async` | Subscribe and download asynchronously | `consumer_subscribe_download_data_offers` |
| POST | `/api/subscribe-download-data-offers` | Subscribe and download synchronously | `consumer_subscribe_download_data_offers` |
| GET | `/api/download-data-offers` | Download already transferred data | `consumer_download_data_offer` |
| GET | `/api/view-download-history` | Download history | `consumer_view_download_history` |
| GET | `/api/view-download-history/{processId}` | Download history details | `consumer_view_download_history` |

`GET /query-data-offers` requires at least `manufacturerPartId` or `bpnNumber`.
Code defaults are `maxLimit=10` and `offset=0`.

## Portal and BPN

Controller: `PortalProxyController`

| Method | Path | Purpose | Permission |
| --- | --- | --- | --- |
| GET | `/api/legal-entities` | Legal entities from Portal/Partner Pool | `consumer_search_connectors` |
| POST | `/api/connectors-discovery` | Connector discovery | `consumer_search_connectors` |
| GET | `/api/unified-bpn-validation/{bpn}` | Validate BPN | `unified_bpn_validation` |

## Contract Agreements

Controller: `ContractManagementController`

| Method | Path | Purpose | Permission |
| --- | --- | --- | --- |
| GET | `/api/contract-agreements/provider` | Provider agreements | `provider_view_contract_agreement` |
| GET | `/api/contract-agreements/consumer` | Consumer agreements | `consumer_view_contract_agreement` |
| POST | `/api/contract-agreements/{negotiationId}/provider/decline` | Provider decline | `provider_delete_contract_agreement` |
| GET | `/api/contract-agreements/{negotiationId}/consumer/decline` | Consumer decline | `consumer_delete_contract_agreement` |
| POST | `/api/contract-agreements/{negotiationId}/provider/cancel` | Provider cancel | `provider_delete_contract_agreement` |
| POST | `/api/contract-agreements/{negotiationId}/consumer/cancel` | Consumer cancel | `consumer_delete_contract_agreement` |

Note: consumer decline is a mutating operation exposed as GET.

## Policies and Policy Hub

Controllers: `PolicyController`, `PolicyHubController`

| Method | Path | Purpose | Permission |
| --- | --- | --- | --- |
| POST | `/api/policy` | Save local policy | `policy_management` |
| PUT | `/api/policy/{uuid}` | Update local policy | `policy_management` |
| GET | `/api/policy/{uuid}` | Read policy | `policy_management` |
| GET | `/api/policy` | List policies | `policy_management` |
| DELETE | `/api/policy/{uuid}` | Delete policy | `policy_management` |
| GET | `/api/policy/is-policy-name-valid` | Validate policy name | `policy_management` |
| GET | `/api/policy-hub/policy-attributes` | Policy Hub attributes | `policyhub_view_policy_attributes` |
| GET | `/api/policy-hub/policy-types` | Policy Hub types | `policyhub_view_policy_types` |
| GET | `/api/policy-hub/policy-content` | Read Policy Hub content | `policyhub_view_policy_content` |
| POST | `/api/policy-hub/policy-content` | Create Policy Hub content | `policyhub_policy_content` |

## Roles and Permissions

Controller: `RoleManagementController`

| Method | Path | Purpose | Permission |
| --- | --- | --- | --- |
| POST | `/api/role` | Save role | `create_role` |
| DELETE | `/api/role/{role}` | Delete role | `delete_role` |
| GET | `/api/role/{role}/permissions` | Permissions for a role | `read_role_permission` |
| POST | `/api/role/{role}/permissions` | Set permissions for a role | `create_role` |
| GET | `/api/user/role/permissions` | Permissions of current JWT roles | authenticated |

## PCF Exchange

Controller: `PcfExchangeController`

| Method | Path | Purpose | Permission |
| --- | --- | --- | --- |
| POST | `/api/pcf/request/{productId}` | Request PCF for an existing offer | `request_for_pcf_value` |
| POST | `/api/pcf/request/nonexistdataoffer` | Request PCF without an existing data offer | `request_for_pcf_value` |
| GET | `/api/pcf/request/{requestId}` | View PCF request | `request_for_pcf_value` |
| POST | `/api/pcf/actionsonrequest` | Provider action on PCF request | `action_on_pcf_request` |
| GET | `/api/pcf/{type}/requests` | PCF history by consumer/provider type | `view_pcf_history` |
| GET | `/api/pcf/productIds/{productId}` | EDC-facing PCF request intake | authenticated, no method annotation |
| PUT | `/api/pcf/productIds/{productId}` | EDC-facing PCF response intake | authenticated, no method annotation |

`GET|PUT /pcf/productIds/{productId}` expect the `Edc-Bpn` header.

## Public/Ops Endpoints

According to `SecurityConfig`, these are public:

- `/api/ping`
- `/api/cache/**`
- `/api/api-docs/**`
- `/api/swagger-ui/**`
- `/api/v3/api-docs/**`
- `/api/actuator/health/readiness`
- `/api/actuator/health/liveness`

The cache endpoints have mutating behavior despite using GET and matching a
public route:

- `/api/cache/clear-memebercompany-bpnnumber`
- `/api/cache/clear-ddtrurl`
- `/api/cache/clear-pcfurl`

## Known OpenAPI/Code Differences

- OpenAPI contains `POST /upload` (`autoUpload`), but static controller
  analysis did not find a matching controller.
- The controller contains `GET /submodels/csvfile/{submodelName}` and
  `GET /cache/clear-pcfurl`; both are missing from the static OpenAPI file.
- OpenAPI does not document PCF `Edc-Bpn` exactly as implemented; the controller
  expects the `Edc-Bpn` header.
- Some pagination parameters differ between OpenAPI and code, for example PCF
  requests.
- The README API table is partially duplicated, outdated, or uses wrong
  prefixes.

