# MIKLINK - MASTER REFACTOR PLAN
**Data creazione**: 2025-01-15  
**Versione**: 2.0  
**Obiettivo**: Refactor completo per sonda unica, test parametrizzati, UX coerente e PDF HTML

---

## 🎯 OBIETTIVI PRINCIPALI

### 1. **Sonda Unica Configurabile**
- **Attuale**: Multi-sonda con selezione in Dashboard
- **Target**: Una sola sonda configurabile, gestita dalle Impostazioni
- **Razionale**: Il telefono si collega al WiFi della sonda MikroTik; gestire più sonde è inutile per il workflow tipico

**Modifiche**:
- `ProbeConfigDao`: aggiungere `getSingleProbe(): Flow<ProbeConfig?>` e `upsertSingle(probe: ProbeConfig)`
- `AppDatabase`: migrazione v7→v8 con constraint `CHECK (probeId = 1)` per forzare single-row
- `DashboardViewModel`: rimuovere `probes`, `selectedProbe`; esporre `currentProbe: Flow<ProbeConfig?>`
- `DashboardScreen`: rimuovere card "Seleziona Sonda"
- `SettingsScreen`: aggiungere sezione "Sonda" con link a `ProbeEditScreen` (riusa esistente)
- `ProbeListScreen`: deprecare (mantieni per compatibilità ma rimuovi da nav principale)
- `NavGraph`: aggiornare route `settings/probe` → riusa `ProbeEditScreen` senza `probeId`

---

### 2. **Monitor Stato Sonda Globale**
- **Attuale**: LED verde/rosso solo in Dashboard
- **Target**: Icona persistente nella TopAppBar principale di tutta l'app

**Implementazione**:
- `MainActivity`: esporre `GlobalProbeStatus` Composable in overlay TopBar
- `AppRepository.observeProbeStatus()`: già presente, riutilizzare
- UI: Badge rosso/verde in alto a destra con icona Router; tap → apre Impostazioni Sonda

---

### 3. **Parametri Test per Cliente/Profilo**

#### 3A. Cliente
- **Campo esistente**: `socketPrefix`, `minLinkRate`, `networkMode`
- **Nuovi campi**: nessuno necessario; tutto già presente
- **Conferma**: auto-incremento ID presa usa `ReportDao.getLastReportForClient()` + `ClientDao.incrementNextIdNumber()`

#### 3B. Profilo Test
- **Campo mancante**: `pingCount: Int` (default 4, range 1-20)
- **Modifica**: aggiungere a `TestProfile` entity
- **UI**: `TestProfileEditScreen` → OutlinedTextField per `pingCount` con validazione
- **Propagazione**: `MikroTikApiService.runPing()` accetta già `count: String`; `AppRepository.runPing()` deve passare il parametro dal profilo

#### 3C. Traceroute
- **Stato**: già implementato (`tracerouteTarget`, `tracerouteMaxHops`, `tracerouteTimeoutMs`)
- **Verifica**: UI edit completa, propagazione in `AppRepository.runTraceroute()` corretta

---

### 4. **Schermata Test: Card, Log e Azioni**

#### 4A. Flusso Attuale (Verificato Corretto)
- **Stato iniziale**: `UiState.Idle` → mostra messaggio "Pronto"
- **Avvio manuale**: pulsante "AVVIA TEST" (autostart rimosso)
- **Durante esecuzione**: `TestInProgressView` con card reattive (Network, TDR, Link, LLDP, Ping, Traceroute)
- **Completamento**: `TestCompletedView` con header PASS/FAIL colorato + card aggregate

#### 4B. Miglioramenti da Applicare
- **Override rete per singolo test**: esporre in UI un ExpansionPanel "Override Configurazione Rete" in `TestExecutionScreen`
  - Switch DHCP/Static temporaneo → collega a `TestViewModel.overrideClientNetwork`
  - Visibile solo durante `UiState.Idle` (prima di avviare)
- **Pulsante "Mostra Dettagli"**: già presente come toggle log/sections
- **Error handling**: tutti i `UiState.Error` già gestiti; migliorare messaggi diagnostici

---

### 5. **Validazione REST API con 192.168.0.251**

#### 5A. Test Manuale Script
- **File**: `scripts/run_mikrotik_commands.ps1` e `.sh`
- **IP target**: `192.168.0.251`
- **Comandi da testare**:
  1. `/rest/ip/dhcp-client/print` con `?.interface` vs `?interface` (verificare quale funziona)
  2. `/rest/interface/disable` + `/rest/interface/enable`
  3. `/rest/ping` con `count=4`
  4. `/rest/tool/traceroute` con `max-hops=30`

#### 5B. Allineamento Retrofit
- **DTO attuale**: `InterfaceNameRequest(@Json(name = "?interface") val interfaceName: String)`
- **Verifica**: se RouterOS richiede `?.interface`, modificare DTO in `MikroTikApiService.kt`
- **Test in-app**: aggiungere pulsante "Verifica Sonda" in `ProbeEditScreen` che esegue smoke test completo

---

### 6. **PDF HTML Personalizzabile**

#### 6A. Nuova Architettura PDF
- **Mantieni**: `PdfGenerator.kt` (fallback PdfDocument nativo per compatibilità)
- **Nuovo**: `HtmlPdfGenerator.kt` con rendering WebView → PDF
- **Template**: `app/src/main/assets/report_template_v2.html` (esteso da quello attuale)

#### 6B. Token Merge Supportati
```html
{{CLIENT_NAME}}
{{SOCKET_ID}}
{{TEST_DATE_TIME}}
{{LOCATION}}
{{OVERALL_STATUS}} → "PASS" o "FAIL"
{{OVERALL_STATUS_CLASS}} → "status-pass" o "status-fail"
{{RESULTS_HTML}} → tabella generata dinamicamente
{{NOTES}}
{{LOGO_BASE64}} → logo aziendale opzionale
```

#### 6C. Tabella Risultati (dinamica)
```html
<table class="test-results">
  <tr>
    <th>Test</th><th>Risultato</th><th>Dettagli</th>
  </tr>
  <!-- Loop per ogni sezione TestSection -->
  <tr>
    <td>{{TEST_TITLE}}</td>
    <td class="{{STATUS_CLASS}}">{{STATUS}}</td>
    <td>{{DETAILS_HTML}}</td>
  </tr>
</table>
```

#### 6D. Mini Grafici (SVG inline)
- **Latenza Ping**: grafico a barre per ogni target
- **Hop Traceroute**: mappa visuale progressiva

#### 6E. Gestione Template
- **UI**: nuova sezione in `SettingsScreen` → "Template PDF"
  - Pulsante "Carica Template" → file picker per `.html`
  - Preview live (WebView non interattivo)
  - Pulsante "Ripristina Default"
- **Storage**: salvare template custom in `app_data/pdf_templates/custom.html`

---

## 📋 CHECKLIST IMPLEMENTAZIONE

### Database (Room v7→v8)
- [ ] `ProbeConfig`: nessuna modifica schema (mantieni multi-row per compatibilità ma usa solo prima)
- [ ] `TestProfile`: aggiungere `pingCount: Int = 4`
- [ ] `ProbeConfigDao`: aggiungere `getSingleProbe()` e `upsertSingle()`
- [ ] `AppDatabase`: migrazione v7→v8 con aggiunta colonna `pingCount`

### Repository Layer
- [ ] `AppRepository`: esporre `currentProbe: Flow<ProbeConfig?>` derivato da `getSingleProbe()`
- [ ] `AppRepository.runPing()`: aggiungere parametro `count: Int` e passare a `PingRequest(address, count.toString())`

### UI Layer - Sonda Unica
- [ ] `DashboardViewModel`: rimuovere `probes` e `selectedProbe`; esporre `currentProbe`
- [ ] `DashboardScreen`: rimuovere card "Seleziona Sonda"; verificare `isTestButtonEnabled` usa `currentProbe != null`
- [ ] `SettingsScreen`: aggiungere card "Sonda" → link a `settings/probe`
- [ ] `NavGraph`: aggiungere route `settings/probe` → `ProbeEditScreen` (carica prima sonda o crea nuova)
- [ ] `ProbeEditViewModel`: modificare init per caricare `getSingleProbe()` se `probeId == -1`

### UI Layer - Test Parametrizzati
- [ ] `TestProfileEditScreen`: aggiungere campo `pingCount` con validazione (1-20)
- [ ] `TestProfileViewModel`: esporre `pingCount` StateFlow
- [ ] `TestViewModel.startTest()`: leggere `profile.pingCount` e passare a `repository.runPing()`

### UI Layer - Monitor Globale
- [ ] `MainActivity`: aggiungere `GlobalProbeStatusBadge` in overlay (top-right corner)
- [ ] `GlobalProbeStatusBadge` Composable: osserva `currentProbe` e `observeProbeStatus()`; mostra LED verde/rosso

### UI Layer - Override Rete Test
- [ ] `TestExecutionScreen`: aggiungere ExpansionPanel sopra pulsante "AVVIA TEST"
- [ ] ExpansionPanel: Switch DHCP/Static + campi IP/Gateway (visibile solo in `UiState.Idle`)
- [ ] Al toggle: settare `viewModel.overrideClientNetwork` con Client temporaneo

### Network - Validazione API
- [ ] Eseguire script `run_mikrotik_commands.ps1` su 192.168.0.251
- [ ] Verificare `?.interface` vs `?interface` per DHCP client print
- [ ] Aggiornare `InterfaceNameRequest` se necessario
- [ ] `ProbeEditScreen`: aggiungere pulsante "Test Completo" che esegue: resource, dhcp-client, disable/enable, ping, traceroute

### PDF HTML
- [ ] Creare `HtmlPdfGenerator.kt` con metodo `generateFromTemplate(template: String, report: Report, client: Client): ByteArray`
- [ ] Estendere template HTML in `assets/report_template_v2.html` con token merge e CSS stile Ubiquiti
- [ ] `SettingsScreen`: sezione "Template PDF" con carica/preview/ripristina
- [ ] `SettingsViewModel`: gestire storage custom template
- [ ] `ReportDetailScreen` e `HistoryScreen`: usare `HtmlPdfGenerator` se template custom presente, altrimenti fallback a `PdfGenerator`

### UX/UI - Stile Ubiquiti
- [ ] `ui/theme/Color.kt`: aggiungere palette Ubiquiti (blu scuro, azzurro, grigi)
- [ ] `ui/theme/Theme.kt`: applicare colori coerenti a tutte le card
- [ ] Tutte le schermate: uniformare RoundedCornerShape (12.dp), elevazione (2.dp), padding (16.dp)
- [ ] IconButton: uniformare size (24.dp)

---

## 🔧 MODIFICHE TECNICHE DETTAGLIATE

### A. Database Migration v7→v8

```kotlin
// In AppDatabase.kt
@Database(
    entities = [Client::class, ProbeConfig::class, TestProfile::class, Report::class],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    // ...existing code...
}

// In DatabaseModule.kt
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Aggiungi colonna pingCount a test_profiles con default 4
        database.execSQL("ALTER TABLE test_profiles ADD COLUMN pingCount INTEGER NOT NULL DEFAULT 4")
    }
}

// In provideAppDatabase()
.addMigrations(MIGRATION_7_8)
.fallbackToDestructiveMigration() // rimuovere dopo test migrazione
```

### B. ProbeConfigDao Esteso

```kotlin
@Dao
interface ProbeConfigDao {
    // ...existing code...
    
    @Query("SELECT * FROM probe_config ORDER BY probeId ASC LIMIT 1")
    fun getSingleProbe(): Flow<ProbeConfig?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSingle(probe: ProbeConfig)
    
    @Query("DELETE FROM probe_config WHERE probeId != :keepId")
    suspend fun deleteAllExcept(keepId: Long)
}
```

### C. AppRepository Modifiche

```kotlin
class AppRepository @Inject constructor(
    // ...existing code...
) {
    // Esponi sonda unica
    val currentProbe: Flow<ProbeConfig?> = probeConfigDao.getSingleProbe()
    
    // Modifica firma runPing
    suspend fun runPing(
        probe: ProbeConfig, 
        target: String, 
        interfaceName: String,
        count: Int = 4 // nuovo parametro
    ): UiState<PingResult> = safeApiCall {
        val resolvedTarget = resolveTargetIp(probe, target, interfaceName)
        if (resolvedTarget.equals("DHCP_GATEWAY", ignoreCase = true)) {
            throw IllegalStateException("DHCP gateway not resolved for interface $interfaceName")
        }
        val api = buildServiceFor(probe)
        api.runPing(PingRequest(address = resolvedTarget, count = count.toString())).first()
    }
}
```

### D. DashboardViewModel Semplificato

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
    
    // RIMOSSO: val probes
    // NUOVO: sonda unica
    val currentProbe: StateFlow<ProbeConfig?> = repository.currentProbe
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val profiles: StateFlow<List<TestProfile>> = testProfileDao.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val selectedClient = MutableStateFlow<Client?>(null)
    // RIMOSSO: val selectedProbe
    val selectedProfile = MutableStateFlow<TestProfile?>(null)
    val socketName = MutableStateFlow("")
    
    val isProbeOnline: StateFlow<Boolean> = currentProbe.flatMapLatest { probe ->
        if (probe == null) flowOf(false)
        else repository.observeProbeStatus(probe)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    // ...existing init logic...
}
```

---

## 🎨 UX/UI SPECIFICATION

### Palette Colori Ubiquiti-Style
```kotlin
// Color.kt
val UbiquitiBlue = Color(0xFF0559C9)
val UbiquitiLightBlue = Color(0xFF4A9AFF)
val UbiquitiDarkGray = Color(0xFF2C3E50)
val UbiquitiLightGray = Color(0xFFECF0F1)
val UbiquitiGreen = Color(0xFF00C896)
val UbiquitiRed = Color(0xFFE74C3C)
```

### Card Standard
- **Shape**: `RoundedCornerShape(12.dp)`
- **Elevation**: `2.dp`
- **Padding interno**: `16.dp`
- **Spacing tra card**: `12.dp`

### TopAppBar
- **containerColor**: `primaryContainer.copy(alpha = 0.3f)`
- **Icone**: size `28.dp`, tint `primary`

### Buttons
- **Primary**: `containerColor = UbiquitiBlue`
- **Success**: `containerColor = UbiquitiGreen`
- **Error**: `containerColor = UbiquitiRed`
- **Height**: `56.dp` per pulsanti principali, `48.dp` per secondari

---

## 📊 METRICHE PREVISTE

### Linee di Codice
- **Da rimuovere**: ~300 (ProbeListScreen deprecata, duplicati)
- **Da aggiungere**: ~800 (HtmlPdfGenerator, UI override, docs)
- **Da modificare**: ~600 (ViewModel, Repository, Screens)
- **Net change**: +500 linee

### Files Modificati
- **Database**: 3 (AppDatabase, ProbeConfigDao, TestProfile)
- **Repository**: 1 (AppRepository)
- **ViewModels**: 4 (Dashboard, Test, Settings, ProbeEdit)
- **Screens**: 6 (Dashboard, Settings, TestExecution, ProbeEdit, TestProfileEdit, MainActivity)
- **Nuovi**: 3 (HtmlPdfGenerator, GlobalProbeStatusBadge, docs)
- **Totale**: 17 files

### Testing
- **Unit test**: +5 (RateParser, isRateOk, HtmlPdfGenerator merge)
- **Integration test**: +2 (API validation, PDF generation)
- **Manual test**: checklist completa (vedi API_VALIDATION.md)

---

## 🚀 ROLLOUT PLAN

### Fase 1: Database + Repository (1-2 ore)
1. Migrazione v7→v8
2. ProbeConfigDao esteso
3. AppRepository modifiche
4. Test migrazione su emulatore

### Fase 2: UI Sonda Unica (2-3 ore)
1. DashboardViewModel semplificato
2. DashboardScreen rimozione card
3. SettingsScreen sezione Sonda
4. ProbeEditViewModel modifica init
5. GlobalProbeStatusBadge

### Fase 3: Parametri Test (1 ora)
1. TestProfile add pingCount
2. TestProfileEditScreen campo
3. TestViewModel propagazione

### Fase 4: PDF HTML (3-4 ore)
1. HtmlPdfGenerator implementazione
2. Template HTML v2 esteso
3. SettingsScreen gestione template
4. Integration con export esistente

### Fase 5: Validazione API (1 ora)
1. Test script su 192.168.0.251
2. Fix DTO se necessario
3. Smoke test in ProbeEditScreen

### Fase 6: UX Polish (2 ore)
1. Palette colori Ubiquiti
2. Uniformazione card/buttons
3. Override rete UI

### Fase 7: Testing & Docs (2 ore)
1. Test completo flusso
2. Fix bug rilevati
3. Documentazione finale

**Tempo totale stimato**: 12-15 ore

---

## ✅ CRITERI DI SUCCESSO

1. **Sonda unica**: Dashboard NON mostra card selezione sonda; badge globale funzionante
2. **Test parametrizzati**: pingCount personalizzabile e propagato a MikroTik
3. **PDF HTML**: Template custom caricabile, preview funzionante, export PDF corretto
4. **API validated**: Script su 192.168.0.251 eseguito con successo, DTO allineato
5. **UX coerente**: Tutte le schermate seguono stile Ubiquiti (palette, shape, spacing)
6. **Zero regression**: Tutti i test esistenti passano; nessun blocco UI; nessuna perdita dati

---

**Fine MASTER_PLAN.md**

