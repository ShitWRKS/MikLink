# MikroTik REST API (RouterOS)

Questa pagina descrive la costruzione del service Retrofit e **gli endpoint effettivamente presenti** nella codebase.

## Base URL e HTTPS

- La URL base è costruita in `MikroTikServiceFactory.createService(probeConfig)`:
  - `scheme = https` se `ProbeConfig.isHttps = true`
  - `scheme = http` altrimenti
  - `baseUrl = "$scheme://${probe.ipAddress}/"`

## Autenticazione

- Basic Auth via header `Authorization: Basic <base64(user:pass)>`.
- L'header viene aggiunto da un interceptor locale nella factory quando `username` o `password` non sono blank.

## Trust-all (solo in HTTPS)

- Se `isHttps = true` la factory configura un SSLContext permissivo (trust-all) e `hostnameVerifier` permissivo **solo per questo client** (vedi ADR-0002).

## Binding Wi‑Fi (quando disponibile)

- `MikroTikServiceProviderImpl` prova a trovare una rete Wi‑Fi attiva e, se presente, passa la `socketFactory` alla factory.

## Endpoint

Definiti in `data/remote/mikrotik/service/MikroTikApiService.kt`:

| Metodo | Path | Funzione |
|---|---|---|
| `POST` | `/rest/system/resource/print` | `getSystemResource(@Body request: ProplistRequest = ProplistRequest(…` |
| `GET` | `/rest/interface/ethernet` | `getEthernetInterfaces(@Query(".proplist") proplist: String = "name"…` |
| `GET` | `/rest/ip/dhcp-client` | `getDhcpClientStatus(@Query("interface") interfaceName: String): Lis…` |
| `POST` | `/rest/ip/dhcp-client/add` | `addDhcpClient(@Body request: DhcpClientAdd): Any` |
| `POST` | `/rest/ip/dhcp-client/enable` | `enableDhcpClient(@Body request: NumbersRequest): Any` |
| `POST` | `/rest/ip/dhcp-client/disable` | `disableDhcpClient(@Body request: NumbersRequest): Any` |
| `GET` | `/rest/ip/address` | `getIpAddresses(@Query(".proplist") proplist: String = ".id,address,…` |
| `POST` | `/rest/ip/address/add` | `addIpAddress(@Body request: IpAddressAdd): Any` |
| `POST` | `/rest/ip/address/remove` | `removeIpAddress(@Body request: NumbersRequest): Any` |
| `GET` | `/rest/ip/route` | `getRoutes(@Query(".proplist") proplist: String = ".id,dst-address,g…` |
| `POST` | `/rest/ip/route/add` | `addRoute(@Body request: RouteAdd): Any` |
| `POST` | `/rest/ip/route/remove` | `removeRoute(@Body request: NumbersRequest): Any` |
| `POST` | `/rest/interface/ethernet/cable-test` | `runCableTest(@Body request: CableTestRequest): List<CableTestResult>` |
| `POST` | `/rest/interface/ethernet/monitor` | `getLinkStatus(@Body request: MonitorRequest): List<MonitorResponse>` |
| `GET` | `/rest/ip/neighbor` | `getIpNeighbors(` |
| `POST` | `/rest/ping` | `runPing(@Body request: PingRequest): List<PingResult>` |
| `POST` | `/rest/tool/speed-test` | `runSpeedTest(` |


> Nota: alcune righe molto lunghe (es. `.proplist`) possono risultare troncate nel dump testuale; fai sempre riferimento al file Kotlin come fonte finale.


## Parsing e Golden tests
- I DTO vengono parsati con **Moshi**.
- Le suite `app/src/test/java/com/app/miklink/data/remote/mikrotik/golden/*` validano che il parsing resti stabile su fixture versionate.
