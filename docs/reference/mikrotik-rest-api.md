# MikroTik REST API (RouterOS)

Questa pagina descrive le chiamate REST usate oggi dalla codebase per interagire con RouterOS.

## Base URL

- Host: configurato tramite `ProbeConfig.ipAddress`
- Protocollo:
  - `http://` se `isHttps = false`
  - `https://` se `isHttps = true` (vedi ADR-0002)

## Autenticazione

- Basic Auth (header `Authorization: Basic ...`)
- Interceptor: `AuthInterceptor`

## Endpoint principali (attuali)

Definiti in `core/data/remote/mikrotik/service/MikroTikApiService.kt`:

- `POST /rest/system/resource/print`
  - usato per ottenere risorse / modello (board-name)

- `GET /rest/interface/ethernet`
  - elenco interfacce Ethernet (proplist default: `name`)

- DHCP client:
  - `GET /rest/ip/dhcp-client`
  - `POST /rest/ip/dhcp-client/add`
  - `POST /rest/ip/dhcp-client/enable`
  - `POST /rest/ip/dhcp-client/disable`

- IP addresses:
  - `GET /rest/ip/address`
  - `POST /rest/ip/address/add`
  - `POST /rest/ip/address/remove`

- Routing:
  - `GET /rest/ip/route`
  - `POST /rest/ip/route/add`
  - `POST /rest/ip/route/remove`

- Test “link / cavo”:
  - `POST /rest/interface/ethernet/cable-test`
  - `POST /rest/interface/ethernet/monitor`

- Neighbor discovery:
  - `GET /rest/ip/neighbor`
    - query: `interface=<name>`
    - default `.proplist` include (tra gli altri) identity, interface-name, system-caps, address, mac-address, ecc.

- `POST /rest/ping`
- `POST /rest/tool/speed-test`

## Note importanti

- Il parsing dei risultati usa **Moshi**.
- La determinazione di TDR support richiede una fonte di verità nel dominio: il vecchio placeholder `core/domain/tdr/TdrCapabilities` è stato rimosso nel Milestone 5 e verrà riprogettato in una futura epic prima di reintrodurre il supporto TDR.
