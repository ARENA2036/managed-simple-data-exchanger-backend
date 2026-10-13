# SDE against the Construct-X local testbed

Runs SDE on your machine against a provider/consumer pair of Construct-X EDCs in Docker.

## 1. Start the Construct-X testbed

In your `constructx-edc` checkout, follow
`edc-controlplane/edc-controlplane-construct-x/local/README.md`:
build the control-plane and data-plane images, `docker compose up`, then run the
Bruno `identities` folder once so both participants are onboarded.

## 2. Start the SDE add-ons

```bash
cd local/construct-x
docker compose -f docker-compose.sde-addons.yaml up -d
```

This adds SDE's database (`localhost:5433`), a Digital Twin Registry (`sde-dtr:4243`)
and a submodel server (`sde-submodel:8090`) to the testbed network.

## 3. Hosts file

SDE runs on the host but has to hand out URLs the Docker containers can use.
Add this line to `C:\Windows\System32\drivers\etc\hosts` (admin editor):

```
127.0.0.1 provider-controlplane consumer-controlplane provider-dataplane sde-dtr sde-submodel
```

## 4. Run SDE with the `constructx` profile

IntelliJ run configuration → Active profiles: `constructx`
(or `-Dspring.profiles.active=constructx`).

Settings are in `modules/sde-core/src/main/resources/application-constructx.properties`.

| Role | Management API (from SDE) | DSP (from the other EDC) | Participant ID |
| --- | --- | --- | --- |
| Provider | `http://provider-controlplane:39010/management` | `http://provider-controlplane:9020/dsp` | `did:web:provider-wallet:user:provider` |
| Consumer | `http://consumer-controlplane:29010/management` | `http://consumer-controlplane:9020/dsp` | `did:web:consumer-wallet:user:consumer` |

## 5. Smoke test (expected state per ticket)

| Step | Works from |
| --- | --- |
| Upload CSV (twin + asset + offer) | CX-2 / CX-3 |
| Consumer: catalog of provider | CX-4 |
| Consumer: download | CX-5 |

Until those tickets land, the failures you see are the known gaps listed in the
SDE architecture doc, not setup errors.
