# MIKLINK - ARCHITETTURA TECNICA DETTAGLIATA
**Versione**: 2.0  
**Data**: 2025-01-15

---

## 📐 ARCHITETTURA OVERVIEW

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐         │
│  │  Composables │  │  ViewModels  │  │  Navigation  │         │
│  │  (Screens)   │  │  (@HiltVM)   │  │  (NavGraph)  │         │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘         │
│         │                  │                  │                  │
└─────────┼──────────────────┼──────────────────┼─────────────────┘
          │                  │                  │
┌─────────▼──────────────────▼──────────────────▼─────────────────┐
│                      DOMAIN LAYER                               │
│  ┌────────────────────────────────────────────────────────┐    │
│  │             AppRepository (Singleton)                   │    │
│  │  • Business Logic (Network Config, Test Execution)      │    │
│  │  • State Management (ProbeStatus, Test Results)         │    │
│  │  • Coordination (DAO + API calls)                       │    │
│  └────────┬───────────────────────────┬────────────────────┘    │
│           │                           │                          │
└───────────┼───────────────────────────┼──────────────────────────┘
            │                           │
    ┌───────▼────────┐         ┌───────▼─────────┐
    │  DATA LAYER    │         │   DATA LAYER    │
    │  (Local)       │         │   (Remote)      │
    │                │         │                 │
    │  Room DB       │         │  Retrofit API   │
    │  • DAOs        │         │  • MikroTik     │
    │  • Entities    │         │  • DTOs         │
    └────────────────┘         └─────────────────┘
```

---

## 🗄️ DATABASE LAYER (Room)

### Schema v8 (Post-Refactor)

#### **Client** (clients)
Configurazione cliente/sede per test di certificazione

| Campo | Tipo | Constraint | Descrizione |
|-------|------|-----------|-------------|
| `clientId` | Long | PK, AUTO | ID univoco cliente |
| `companyName` | String | NOT NULL | Nome azienda |
| `location` | String? | NULL | Sede (default "Sede") |
| `notes` | String? | NULL | Note libere |
| `networkMode` | String | NOT NULL | "DHCP" o "STATIC" |
| `staticIp` | String? | NULL | IP statico (se STATIC) |
| `staticSubnet` | String? | NULL | Subnet mask (legacy) |
| `staticGateway` | String? | NULL | Gateway (se STATIC) |
| `staticCidr` | String? | NULL | CIDR completo (preferito) |
| `minLinkRate` | String | DEFAULT "1G" | Soglia min per PASS ("10M","100M","1G","10G") |
| `socketPrefix` | String | DEFAULT "" | Prefisso ID presa (es. "PRT-") |
| `nextIdNumber` | Int | DEFAULT 1 | Contatore auto-incremento |

**Business Rules**:
- `socketPrefix` + `nextIdNumber` generano ID univoci (es. "PRT-001")
- `nextIdNumber` incrementato automaticamente dopo ogni test salvato
- `minLinkRate` usato in `TestViewModel.isRateOk()` per validazione link

---

#### **ProbeConfig** (probe_config)
Configurazione sonda MikroTik (SINGLE ROW dopo refactor)

| Campo | Tipo | Constraint | Descrizione |
|-------|------|-----------|-------------|
| `probeId` | Long | PK, AUTO | ID sonda (sempre 1 post-refactor) |
| `name` | String | NOT NULL | Nome identificativo |
| `ipAddress` | String | NOT NULL | IP della sonda |
| `username` | String | NOT NULL | Username API RouterOS |
| `password` | String | NOT NULL | Password API |
| `testInterface` | String | NOT NULL | Interfaccia test (es. "ether2") |
| `isOnline` | Boolean | DEFAULT false | Cache stato (aggiornato da polling) |
| `modelName` | String? | NULL | Board name (es. "RB4011iGS+RM") |
| `tdrSupported` | Boolean | DEFAULT false | Se supporta Cable-Test |
| `isHttps` | Boolean | DEFAULT false | Usa HTTPS invece di HTTP |

**Business Rules**:
- Post-refactor: solo prima riga usata; `ProbeConfigDao.getSingleProbe()` ritorna `LIMIT 1`
- `isOnline` aggiornato da `AppRepository.observeProbeStatus()` ogni 15s
- `tdrSupported` calcolato da `Compatibility.isTdrSupported(modelName)` durante verifica

---

#### **TestProfile** (test_profiles)
Template di test configurabili

| Campo | Tipo | Constraint | Descrizione |
|-------|------|-----------|-------------|
| `profileId` | Long | PK, AUTO | ID profilo |
| `profileName` | String | NOT NULL | Nome profilo |
| `profileDescription` | String? | NULL | Descrizione |
| `runTdr` | Boolean | DEFAULT false | Esegui Cable-Test (TDR) |
| `runLinkStatus` | Boolean | DEFAULT true | Verifica stato link |
| `runLldp` | Boolean | DEFAULT false | Discovery LLDP/CDP |
| `runPing` | Boolean | DEFAULT false | Test ping |
| `pingTarget1` | String? | NULL | Target ping 1 (IP o "DHCP_GATEWAY") |
| `pingTarget2` | String? | NULL | Target ping 2 |
| `pingTarget3` | String? | NULL | Target ping 3 |
| `pingCount` | Int | DEFAULT 4 | **NUOVO** Numero ping per target |
| `runTraceroute` | Boolean | DEFAULT false | Esegui traceroute |
| `tracerouteTarget` | String? | NULL | Target traceroute |
| `tracerouteMaxHops` | Int | DEFAULT 30 | Max hop traceroute |
| `tracerouteTimeoutMs` | Int | DEFAULT 3000 | Timeout per hop (ms) |

**Business Rules**:
- `pingCount` range valido: 1-20 (validato in UI)
- `pingTarget*` supporta "DHCP_GATEWAY" (risolto runtime da `AppRepository.resolveTargetIp()`)
- Profili default creati in `DatabaseModule.onCreate()`: "Full Test", "Quick Test"

---

#### **Report** (test_reports)
Risultati test salvati

| Campo | Tipo | Constraint | Descrizione |
|-------|------|-----------|-------------|
| `reportId` | Long | PK, AUTO | ID report |
| `clientId` | Long? | FK → clients | Cliente associato |
| `timestamp` | Long | NOT NULL | Timestamp Unix (ms) |
| `socketName` | String? | NULL | ID presa testata |
| `notes` | String? | NULL | Note tecniche |
| `probeName` | String? | NULL | Nome sonda usata (cache) |
| `profileName` | String? | NULL | Nome profilo usato (cache) |
| `overallStatus` | String | NOT NULL | "PASS" o "FAIL" |
| `resultsJson` | String | NOT NULL | JSON completo risultati |

**JSON Structure** (`resultsJson`):
```json
{
  "network": {
    "mode": "DHCP",
    "interfaceName": "ether2",
    "address": "192.168.1.100/24",
    "gateway": "192.168.1.1",
    "dns": "8.8.8.8",
    "message": "DHCP lease acquisita"
  },
  "link": {
    "status": "link-ok",
    "rate": "1Gbps"
  },
  "lldp": {
    "identity": "SW-Core-01",
    "interfaceName": "ge-0/0/24",
    "systemCaps": "bridge,router",
    "discoveredBy": "lldp"
  },
  "ping_8.8.8.8": {
    "avgRtt": "12ms"
  },
  "ping_DHCP_GATEWAY": {
    "avgRtt": "2ms"
  },
  "traceroute": [
    {"hop": "1", "host": "192.168.1.1", "avgRtt": "1ms"},
    {"hop": "2", "host": "10.0.0.1", "avgRtt": "5ms"}
  ],
  "tdr": {
    "status": "open",
    "cablePairs": [
      {"pair": "1-2", "status": "open", "length": "45m"},
      {"pair": "3-6", "status": "short", "length": "12m"}
    ]
  }
}
## 📌 Problemi correnti (audit 2025-12-09) e azioni raccomandate

Questo documento riflette lo stato dell'applicazione dopo un audit completo eseguito il 09/12/2025. Di seguito i problemi che richiedono attenzione immediata e le azioni consigliate:

1) Errore KSP / compilazione in PdfGeneratorIText
    - Sintomo: `PdfGeneratorIText.kt` causa errori di compilazione (es. "expecting '->'" e "missing '}'") durante il build (vedi `build_log_utf8.txt`).
    - Impatto: impedisce task `:app:kspDebugKotlin` / compilazione completa → build CI fallisce.
    - Azione consigliata (immediata): correggere la sintassi in `PdfGeneratorIText.kt` oppure introdurre temporaneamente un wrapper `PdfGenerator` compatibile che delega a `PdfGeneratorIText` per ripristinare la stabilità CI.

2) Discrepanza tra test e build (PdfGenerator legacy vs iText)
    - Stato: i test unitari più recenti sono stati aggiornati per usare `PdfGeneratorIText` (vedi `app/src/test/.../PdfGeneratorTest.kt`) e alcuni snapshot di test risultano verdi. Tuttavia, ci sono riferimenti/artefatti legacy (`PdfGenerator`) che possono creare incoerenze e fallimenti in ambienti diversi.
    - Azione consigliata: scegliere una strategia (ad es. rimuovere definitivamente legacy e consolidare su `PdfGeneratorIText`, o creare un adapter `PdfGenerator`→`PdfGeneratorIText`) e applicare la scelta a test e codice di produzione in modo coerente.

3) File sensibili e artefatti nella VCS
    - Esempi: file `key` presente alla radice del repo; numerosi file `.class`/`.dex` nel progetto elencati in `project_structure.txt`.
    - Impatto: rischio sicurezza, repository gonfio.
    - Azione consigliata: rimuovere file sensibili, aggiornare `.gitignore`, e pianificare pulizia della storia Git (BFG/git-filter-repo) con approvazione del team.

4) Altri punti da verificare
    - Controllare riferimenti non risolti e coerenza tipale (es. `runSpeedTest` in `TestViewModel` — alcune snapshot indicano test OK, altre mostrano errori di compilazione). Rivedere i log (`unit_test_compile.log`, `compile_errors.txt`) per ricostruire una diagnosi completa.

Questa sezione va mantenuta aggiornata — aggiungere qui gli esiti dopo ogni fix-commit e il riferimento agli issue/PR che risolvono i problemi.
```

---

### DAOs

#### **ProbeConfigDao**
```kotlin
@Dao
interface ProbeConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(probe: ProbeConfig)
    
    @Update
    suspend fun update(probe: ProbeConfig)
    
    @Delete
    suspend fun delete(probe: ProbeConfig)
    
    @Query("SELECT * FROM probe_config ORDER BY name ASC")
    fun getAllProbes(): Flow<List<ProbeConfig>>
    
    @Query("SELECT * FROM probe_config WHERE probeId = :id")
    fun getProbeById(id: Long): Flow<ProbeConfig?>
    
    // NUOVO POST-REFACTOR
    @Query("SELECT * FROM probe_config ORDER BY probeId ASC LIMIT 1")
    fun getSingleProbe(): Flow<ProbeConfig?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSingle(probe: ProbeConfig)
}
```

#### **ClientDao**
```kotlin
@Dao
interface ClientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(client: Client)
    
    @Update
    suspend fun update(client: Client)
    
    @Delete
    suspend fun delete(client: Client)
    
    @Query("SELECT * FROM clients ORDER BY companyName ASC")
    fun getAllClients(): Flow<List<Client>>
    
    @Query("SELECT * FROM clients WHERE clientId = :id")
    fun getClientById(id: Long): Flow<Client?>
    
    @Query("UPDATE clients SET nextIdNumber = nextIdNumber + 1 WHERE clientId = :id")
    suspend fun incrementNextIdNumber(id: Long)
}
```

---

## 🌐 NETWORK LAYER (Retrofit + MikroTik API)

### Service Configuration

#### **Base Setup** (NetworkModule)
```kotlin
@Provides
@Singleton
fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
    val trustAllCerts = arrayOf<TrustManager>(createUnsafeTrustManager())
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustAllCerts, java.security.SecureRandom())
    
    return OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true } // Trust self-signed certs
        .build()
}

@Provides
@Singleton
fun provideRetrofitBuilder(moshi: Moshi): Retrofit.Builder {
    return Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi))
}
```

#### **Per-Probe Service** (AppRepository)
```kotlin
private suspend fun buildServiceFor(probe: ProbeConfig): MikroTikApiService {
    val protocol = if (probe.isHttps) "https://" else "http://"
    val baseUrl = "$protocol${probe.ipAddress}/"
    
    val wifiNetwork = findWifiNetwork()
    
    val authInterceptor = okhttp3.Interceptor { chain ->
        val original = chain.request()
        val req = original.newBuilder()
            .header("Authorization", Credentials.basic(probe.username, probe.password))
            .build()
        chain.proceed(req)
    }
    
    val clientBuilder = baseOkHttpClient.newBuilder()
        .addInterceptor(authInterceptor)
    
    if (wifiNetwork != null) {
        clientBuilder.socketFactory(wifiNetwork.socketFactory)
    }
    
    val client = clientBuilder.build()
    
    return retrofitBuilder
        .baseUrl(baseUrl)
        .client(client)
        .build()
        .create(MikroTikApiService::class.java)
}
```

**Key Features**:
- **Per-sonda isolation**: ogni sonda ha credenziali e client dedicati
- **WiFi binding**: usa `socketFactory` della rete WiFi attiva per forzare routing
- **TLS trust-all**: supporto certificati self-signed MikroTik
- **Auth**: Basic Auth header per ogni richiesta

---

### MikroTik REST API Endpoints

#### **System Resource**
```kotlin
@POST("/rest/system/resource/print")
suspend fun getSystemResource(@Body request: ProplistRequest): List<SystemResource>

// Request
data class ProplistRequest(@Json(name = ".proplist") val proplist: List<String>)

// Response
data class SystemResource(@Json(name = "board-name") val boardName: String)
```

#### **DHCP Client**
```kotlin
@POST("/rest/ip/dhcp-client/print")
suspend fun getDhcpClientStatus(@Body request: InterfaceNameRequest): List<DhcpClientStatus>

@POST("/rest/ip/dhcp-client/add")
suspend fun addDhcpClient(@Body request: DhcpClientAdd): Any

@POST("/rest/ip/dhcp-client/enable")
suspend fun enableDhcpClient(@Body request: NumbersRequest): Any

@POST("/rest/ip/dhcp-client/disable")
suspend fun disableDhcpClient(@Body request: NumbersRequest): Any

// DTOs
data class InterfaceNameRequest(@Json(name = "?interface") val interfaceName: String)
data class DhcpClientAdd(
    @Json(name = "interface") val `interface`: String,
    @Json(name = "use-peer-dns") val usePeerDns: String = "yes"
)
data class NumbersRequest(@Json(name = "numbers") val numbers: String)
```

**NOTA**: Verificare se RouterOS richiede `?.interface` o `?interface` tramite test su 192.168.0.251

#### **Interface Monitor**
```kotlin
@POST("/rest/interface/ethernet/monitor")
suspend fun getLinkStatus(@Body request: MonitorRequest): List<MonitorResponse>

data class MonitorRequest(
    @Json(name = "numbers") val numbers: String,
    @Json(name = "once") val once: Boolean = true
)

data class MonitorResponse(
    val status: String, // "link-ok", "no-link", "running"
    val rate: String?   // "1Gbps", "100Mbps", null se down
)
```

#### **Cable Test (TDR)**
```kotlin
@POST("/rest/interface/ethernet/cable-test")
suspend fun runCableTest(@Body request: CableTestRequest): List<CableTestResult>

data class CableTestRequest(@Json(name = "numbers") val numbers: String)

data class CableTestResult(
    @Json(name = "cable-pairs") val cablePairs: List<Map<String, String>>,
    val status: String
)

// Example cablePairs entry:
// {"pair": "1-2", "status": "open", "length": "45m"}
```

#### **Ping**
```kotlin
@POST("/rest/ping")
suspend fun runPing(@Body request: PingRequest): List<PingResult>

data class PingRequest(
    val address: String,
    val count: String = "4" // MODIFICATO per supportare pingCount
)

data class PingResult(@Json(name = "avg-rtt") val avgRtt: String?)
```

#### **Traceroute**
```kotlin
@POST("/rest/tool/traceroute")
suspend fun runTraceroute(@Body request: TracerouteRequest): List<TracerouteHop>

data class TracerouteRequest(
    val address: String,
    @Json(name = "max-hops") val maxHops: String = "30",
    val timeout: String = "3000ms"
)

data class TracerouteHop(
    val hop: String? = null,
    val host: String? = null,
    @Json(name = "avg-rtt") val avgRtt: String? = null
)
```

---

## 💼 BUSINESS LOGIC LAYER (AppRepository)

### Core Responsibilities

1. **Network Configuration**
   - `applyClientNetworkConfig()`: configura DHCP o Static su interfaccia test
   - `getCurrentInterfaceIpConfig()`: legge config attuale
   - `resolveTargetIp()`: risolve "DHCP_GATEWAY" in IP effettivo

2. **Test Execution**
   - `runCableTest()`: TDR cable test
   - `getLinkStatus()`: stato e velocità link
   - `getNeighborsForInterface()`: LLDP/CDP discovery
   - `runPing()`: ping con count configurabile
   - `runTraceroute()`: traceroute con max-hops e timeout

3. **Probe Management**
   - `checkProbeConnection()`: verifica raggiungibilità e board-name
   - `observeProbeStatus()`: polling stato online/offline (15s interval)
   - `observeAllProbesWithStatus()`: polling multi-sonda (deprecato post-refactor)

### Key Methods (Post-Refactor)

```kotlin
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    val clientDao: ClientDao,
    val probeConfigDao: ProbeConfigDao,
    val testProfileDao: TestProfileDao,
    val reportDao: ReportDao,
    private val retrofitBuilder: Retrofit.Builder,
    private val baseOkHttpClient: OkHttpClient
) {
    
    // NUOVO: sonda unica
    val currentProbe: Flow<ProbeConfig?> = probeConfigDao.getSingleProbe()
    
    // MODIFICATO: accept count parameter
    suspend fun runPing(
        probe: ProbeConfig,
        target: String,
        interfaceName: String,
        count: Int = 4
    ): UiState<PingResult> = safeApiCall {
        val resolvedTarget = resolveTargetIp(probe, target, interfaceName)
        if (resolvedTarget.equals("DHCP_GATEWAY", ignoreCase = true)) {
            throw IllegalStateException("DHCP gateway not resolved")
        }
        val api = buildServiceFor(probe)
        api.runPing(PingRequest(address = resolvedTarget, count = count.toString())).first()
    }
    
    // ...other methods unchanged...
}
```

---

## 🎨 PRESENTATION LAYER

### ViewModel Pattern (MVVM)

Tutti i ViewModel seguono il pattern:
1. **Inject dependencies** via Hilt constructor
2. **Expose StateFlows** per UI reattiva
3. **Business logic** delegata a Repository
4. **viewModelScope.launch(Dispatchers.IO)** per chiamate async

#### **DashboardViewModel** (Post-Refactor)
```kotlin
@HiltViewModel
class DashboardViewModel @Inject constructor(
    clientDao: ClientDao,
    probeConfigDao: ProbeConfigDao,
    testProfileDao: TestProfileDao,
    private val reportDao: ReportDao,
    private val repository: AppRepository
) : ViewModel() {
    
    val clients: StateFlow<List<Client>> = clientDao.getAllClients()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // MODIFICATO: single probe instead of list
    val currentProbe: StateFlow<ProbeConfig?> = repository.currentProbe
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val profiles: StateFlow<List<TestProfile>> = testProfileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val selectedClient = MutableStateFlow<Client?>(null)
    val selectedProfile = MutableStateFlow<TestProfile?>(null)
    val socketName = MutableStateFlow("")
    
    val isProbeOnline: StateFlow<Boolean> = currentProbe.flatMapLatest { probe ->
        if (probe == null) flowOf(false)
        else repository.observeProbeStatus(probe)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    init {
        viewModelScope.launch {
            selectedClient.collect { client ->
                if (client != null) {
                    val lastReport = reportDao.getLastReportForClient(client.clientId)
                    val nextNumber = if (lastReport == null) 1
                    else {
                        val lastNumber = lastReport.socketName?.removePrefix(client.socketPrefix)?.toIntOrNull() ?: 0
                        lastNumber + 1
                    }
                    socketName.value = "${client.socketPrefix}${String.format("%03d", nextNumber)}"
                } else {
                    socketName.value = ""
                }
            }
        }
    }
}
```

#### **TestViewModel**
```kotlin
@HiltViewModel
class TestViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val clientDao: ClientDao,
    private val probeDao: ProbeConfigDao,
    private val profileDao: TestProfileDao,
    private val reportDao: ReportDao,
    private val repository: AppRepository,
    private val moshi: Moshi
) : ViewModel() {
    
    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log = _log.asStateFlow()
    
    private val _uiState = MutableStateFlow<UiState<Report>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()
    
    private val _sections = MutableStateFlow<List<TestSection>>(emptyList())
    val sections: StateFlow<List<TestSection>> = _sections.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    var overrideClientNetwork: Client? = null
    
    fun startTest() {
        if (_isRunning.value) {
            addLog("Ignorato startTest(): test già in esecuzione")
            return
        }
        viewModelScope.launch {
            _isRunning.value = true
            _log.value = emptyList()
            _uiState.value = UiState.Loading
            _sections.value = emptyList()
            
            // Carica parametri navigazione
            val clientId = savedStateHandle.get<Long>("clientId") ?: -1L
            val probeId = savedStateHandle.get<Long>("probeId") ?: -1L
            val profileId = savedStateHandle.get<Long>("profileId") ?: -1L
            val socketName = android.net.Uri.decode(savedStateHandle.get<String>("socketName") ?: "")
            
            // Validation
            if (clientId <= 0 || probeId <= 0 || profileId <= 0) {
                _uiState.value = UiState.Error("Parametri non validi")
                _isRunning.value = false
                return@launch
            }
            
            // Carica entità
            val client = clientDao.getClientById(clientId).firstOrNull()
            val probe = probeDao.getProbeById(probeId).firstOrNull()
            val profile = profileDao.getProfileById(profileId).firstOrNull()
            
            if (client == null || probe == null || profile == null) {
                _uiState.value = UiState.Error("Dati non trovati")
                _isRunning.value = false
                return@launch
            }
            
            val testResults = mutableMapOf<String, Any>()
            var overallStatus = "PASS"
            
            try {
                addLog("--- INIZIO TEST ---")
                
                // 1. Network Config
                when (val apply = repository.applyClientNetworkConfig(probe, client, overrideClientNetwork)) {
                    is UiState.Success -> {
                        addLog("Rete: ${apply.data.mode} ${apply.data.address ?: ""}")
                        testResults["network"] = apply.data
                        upsertSection(TestSection(
                            category = TestSectionCategory.INFO,
                            type = TestSectionType.NETWORK,
                            title = "Rete",
                            status = "PASS",
                            details = listOf(
                                TestDetail("Modalità", apply.data.mode),
                                TestDetail("Indirizzo", apply.data.address ?: "-"),
                                TestDetail("Gateway", apply.data.gateway ?: "-")
                            )
                        ))
                    }
                    is UiState.Error -> {
                        addLog("Configurazione rete: FALLITA (${apply.message})")
                        overallStatus = "FAIL"
                    }
                    else -> {}
                }
                
                // 2. TDR (se supportato)
                if (profile.runTdr && probe.tdrSupported) {
                    addLog("Esecuzione TDR...")
                    when (val tdr = repository.runCableTest(probe, probe.testInterface)) {
                        is UiState.Success -> {
                            addLog("TDR: SUCCESSO")
                            testResults["tdr"] = tdr.data
                            upsertSection(TestSection(
                                category = TestSectionCategory.TEST,
                                type = TestSectionType.TDR,
                                title = "TDR",
                                status = "PASS",
                                details = emptyList()
                            ))
                        }
                        is UiState.Error -> {
                            addLog("TDR: FALLITO (${tdr.message})")
                            overallStatus = "FAIL"
                        }
                        else -> {}
                    }
                }
                
                // 3. Link Status
                if (profile.runLinkStatus) {
                    addLog("Verifica stato link...")
                    when (val link = repository.getLinkStatus(probe, probe.testInterface)) {
                        is UiState.Success -> {
                            val data = link.data
                            addLog("Link: ${data.status} @ ${data.rate ?: "?"}")
                            testResults["link"] = data
                            
                            val isDown = data.status.contains("down", true)
                            val okRate = if (!isDown) isRateOk(data.rate, client.minLinkRate) else false
                            
                            if (isDown) {
                                addLog("Link DOWN rilevato → FAIL")
                                overallStatus = "FAIL"
                                upsertSection(TestSection(
                                    category = TestSectionCategory.TEST,
                                    type = TestSectionType.LINK,
                                    title = "Link",
                                    status = "FAIL",
                                    details = listOf(TestDetail("Status", data.status))
                                ))
                                finalizeAndEmit(client, probe, profile, socketName, overallStatus, testResults, "Link DOWN")
                                return@launch
                            }
                            
                            if (!okRate) {
                                addLog("Velocità link < ${client.minLinkRate} → FAIL")
                                overallStatus = "FAIL"
                            }
                            
                            upsertSection(TestSection(
                                category = TestSectionCategory.TEST,
                                type = TestSectionType.LINK,
                                title = "Link",
                                status = if (okRate) "PASS" else "FAIL",
                                details = listOf(
                                    TestDetail("Status", data.status),
                                    TestDetail("Rate", data.rate ?: "-")
                                )
                            ))
                        }
                        is UiState.Error -> {
                            addLog("Link: FALLITO (${link.message})")
                            overallStatus = "FAIL"
                        }
                        else -> {}
                    }
                }
                
                // 4. LLDP/CDP
                if (profile.runLldp) {
                    addLog("Discovery LLDP/CDP...")
                    when (val lldp = repository.getNeighborsForInterface(probe, probe.testInterface)) {
                        is UiState.Success -> {
                            val neighbors = lldp.data
                            if (neighbors.isNotEmpty()) {
                                val neighbor = neighbors.first()
                                addLog("LLDP/CDP: ${neighbor.identity ?: "Unknown"}")
                                testResults["lldp"] = neighbor
                                upsertSection(TestSection(
                                    category = TestSectionCategory.INFO,
                                    type = TestSectionType.LLDP,
                                    title = "LLDP/CDP",
                                    status = "PASS",
                                    details = listOf(
                                        TestDetail("Identity", neighbor.identity ?: "-"),
                                        TestDetail("Porta", neighbor.interfaceName ?: "-")
                                    )
                                ))
                            }
                        }
                        is UiState.Error -> {
                            addLog("LLDP/CDP: FALLITO (${lldp.message})")
                        }
                        else -> {}
                    }
                }
                
                // 5. Ping (usa profile.pingCount)
                if (profile.runPing) {
                    val targets = listOfNotNull(profile.pingTarget1, profile.pingTarget2, profile.pingTarget3)
                        .filter { it.isNotBlank() }
                    
                    if (targets.isNotEmpty()) {
                        addLog("Ping verso ${targets.size} target (count=${profile.pingCount})...")
                        var allPingsPassed = true
                        
                        for (target in targets) {
                            val resolved = repository.resolveTargetIp(probe, target, probe.testInterface)
                            if (resolved.equals("DHCP_GATEWAY", true)) {
                                addLog("Ping $target: SALTATO (gateway non risolto)")
                                continue
                            }
                            
                            // USA profile.pingCount
                            when (val ping = repository.runPing(probe, resolved, probe.testInterface, profile.pingCount)) {
                                is UiState.Success -> {
                                    val avgRtt = ping.data.avgRtt ?: "N/A"
                                    addLog("Ping $resolved: $avgRtt")
                                    testResults["ping_$target"] = ping.data
                                }
                                is UiState.Error -> {
                                    addLog("Ping $resolved: FALLITO")
                                    allPingsPassed = false
                                }
                                else -> {}
                            }
                        }
                        
                        upsertSection(TestSection(
                            category = TestSectionCategory.TEST,
                            type = TestSectionType.PING,
                            title = "Ping",
                            status = if (allPingsPassed) "PASS" else "FAIL",
                            details = emptyList()
                        ))
                        
                        if (!allPingsPassed) overallStatus = "FAIL"
                    }
                }
                
                // 6. Traceroute
                if (profile.runTraceroute) {
                    val target = profile.tracerouteTarget?.takeIf { it.isNotBlank() }
                    if (target != null) {
                        addLog("Traceroute verso $target...")
                        val resolved = repository.resolveTargetIp(probe, target, probe.testInterface)
                        
                        if (!resolved.equals("DHCP_GATEWAY", true)) {
                            when (val tr = repository.runTraceroute(
                                probe, resolved, probe.testInterface,
                                profile.tracerouteMaxHops, profile.tracerouteTimeoutMs
                            )) {
                                is UiState.Success -> {
                                    val hops = tr.data
                                    val reached = hops.lastOrNull()?.host?.isNotBlank() == true
                                    addLog("Traceroute: ${if (reached) "SUCCESSO" else "PARZIALE"}")
                                    testResults["traceroute"] = hops
                                    
                                    upsertSection(TestSection(
                                        category = TestSectionCategory.TEST,
                                        type = TestSectionType.TRACEROUTE,
                                        title = "Traceroute",
                                        status = if (reached) "PASS" else "FAIL",
                                        details = emptyList()
                                    ))
                                    
                                    if (!reached) overallStatus = "FAIL"
                                }
                                is UiState.Error -> {
                                    addLog("Traceroute: FALLITO (${tr.message})")
                                    overallStatus = "FAIL"
                                }
                                else -> {}
                            }
                        }
                    }
                }
                
            } catch (e: Exception) {
                addLog("ERRORE: ${e.message}")
                overallStatus = "FAIL"
            }
            
            addLog("--- TEST COMPLETATO ---")
            _isRunning.value = false
            finalizeAndEmit(client, probe, profile, socketName, overallStatus, testResults, null)
        }
    }
    
    private fun isRateOk(rate: String?, min: String): Boolean {
        val actual = RateParser.parseToMbps(rate)
        val threshold = RateParser.parseToMbps(min)
        
        if (actual == 0 && !rate.isNullOrBlank()) {
            addLog("ATTENZIONE: Formato velocità non riconosciuto ('$rate') → FAIL")
        } else if (rate.isNullOrBlank()) {
            addLog("ATTENZIONE: Velocità non disponibile → FAIL")
        }
        
        return actual >= threshold
    }
    
    private fun finalizeAndEmit(
        reportClient: Client, reportProbe: ProbeConfig, reportProfile: TestProfile,
        socketName: String, overallStatus: String, testResults: Map<String, Any>, notes: String?
    ) {
        val built = buildSectionsFromResults(testResults)
        _sections.value = built
        
        val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter = moshi.adapter<Map<String, Any>>(type)
        val resultsJson = adapter.toJson(testResults)
        
        val report = Report(
            clientId = reportClient.clientId,
            timestamp = System.currentTimeMillis(),
            socketName = socketName,
            notes = notes,
            probeName = reportProbe.name,
            profileName = reportProfile.profileName,
            overallStatus = overallStatus,
            resultsJson = resultsJson
        )
        
        _uiState.value = UiState.Success(report)
    }
    
    fun saveReportToDb(report: Report) {
        viewModelScope.launch {
            reportDao.insert(report)
            report.clientId?.let {
                clientDao.incrementNextIdNumber(it)
            }
        }
    }
    
    private fun addLog(message: String) {
        _log.value = _log.value + message
    }
}
```

---

## 🔒 SECURITY & NETWORKING

### TLS Trust-All (Self-Signed Certificates)
```kotlin
private fun createUnsafeTrustManager(): X509TrustManager {
    return object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }
}
```

**Rationale**: MikroTik sonde usano certificati self-signed per HTTPS; trust-all necessario per comunicazione.

**Security Note**: Usare SOLO per comunicazione con sonde su rete locale; NON esporre a Internet.

---

### WiFi Network Binding
```kotlin
private suspend fun findWifiNetwork(): Network? = withContext(Dispatchers.IO) {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val active = connectivityManager.activeNetwork
    val activeCaps = active?.let { connectivityManager.getNetworkCapabilities(it) }
    
    if (activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) return@withContext active
    
    connectivityManager.allNetworks.firstOrNull { net ->
        connectivityManager.getNetworkCapabilities(net)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }
}
```

**Rationale**: Forza routing su WiFi anche se cellulare è default; critico per raggiungere sonda MikroTik.

---

## 📊 STATE MANAGEMENT

### UiState Pattern
```kotlin
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val exception: Throwable? = null) : UiState<Nothing>()
}
```

**Usage**: Tutte le operazioni async (DB, API) ritornano `UiState<T>` per gestione uniforme errori/loading.

### StateFlow Lifecycle
- **WhileSubscribed(5000)**: Flow stoppa 5s dopo che UI non osserva più
- **Dispatcher.IO**: tutte le operazioni DB/API su IO dispatcher
- **viewModelScope**: cancellazione automatica quando ViewModel distrutto

---

**Fine ARCHITECTURE.md**

