# MIKLINK - DUPLICATES & OBSOLETE CODE CLEANUP
**Data**: 2025-01-15  
**Obiettivo**: Rimozione codice duplicato, file obsoleti e funzioni inutilizzate

---

## 🔍 DUPLICATI RILEVATI

### 1. **Compatibility.kt** (DUPLICATO)

**Files**:
- ✅ `app/src/main/java/com/app/miklink/utils/Compatibility.kt` (MANTIENI)
- ❌ `app/src/main/java/com/app/miklink/util/Compatibility.kt` (ELIMINA)

**Stato**: `util/Compatibility.kt` contiene solo commento "File rimosso"

**Azione**:
```powershell
Remove-Item "C:\Users\dot\AndroidStudioProjects\MikLink\app\src\main\java\com\app\miklink\util\Compatibility.kt"
```

---

### 2. **SettingsScreen.kt** (DUPLICATO)

**Files**:
- ❌ `app/src/main/java/com/app/miklink/ui/SettingsScreen.kt` (ELIMINA - legacy)
- ✅ `app/src/main/java/com/app/miklink/ui/settings/SettingsScreen.kt` (MANTIENI - attivo)

**Stato**: Legacy file contiene solo commento "Legacy file removed"

**Azione**:
```powershell
Remove-Item "C:\Users\dot\AndroidStudioProjects\MikLink\app\src\main\java\com\app\miklink\ui\SettingsScreen.kt"
```

---

### 3. **AuthInterceptor.kt** (NON USATO)

**File**: `app/src/main/java/com/app/miklink/data/network/AuthInterceptor.kt`

**Problema**: Classe Singleton con metodo `setCredentials()`, ma MAI usata.

**Stato attuale**: `AppRepository.buildServiceFor()` crea interceptor inline ogni volta:
```kotlin
val authInterceptor = okhttp3.Interceptor { chain ->
    val original = chain.request()
    val req = original.newBuilder()
        .header("Authorization", Credentials.basic(probe.username, probe.password))
        .build()
    chain.proceed(req)
}
```

**Azione**: MANTIENI per ora (possibile uso futuro), ma documentare come "unused".

---

### 4. **MikroTikDto.kt** (PARZIALMENTE DUPLICATO)

**File**: `app/src/main/java/com/app/miklink/data/network/dto/MikroTikDto.kt`

**Contenuto**:
```kotlin
data class SystemResourceResponse(@Json(name = "board-name") val boardName: String)
data class EthernetInterfaceResponse(@Json(name = "name") val name: String)
// Note: ProbeCheckResult è definito in AppRepository, non duplicare qui
```

**Problema**: DTO definiti qui MAI usati; `MikroTikApiService.kt` ha DTO inline.

**Azione**: ELIMINA file o MERGE con `MikroTikApiService.kt`.

**Raccomandazione**: MERGE - spostare tutte le data class DTO in questo file, pulire `MikroTikApiService.kt`.

---

### 5. **ProbeCheckResult** (POTENZIALE DUPLICATO)

**Locations**:
- ✅ `AppRepository.kt`: `sealed class ProbeCheckResult { data class Success(...), data class Error(...) }`
- ❌ `MikroTikDto.kt`: Commento "Non duplicare qui"

**Stato**: Nessun duplicato effettivo, solo warning comment.

**Azione**: Nessuna (già risolto).

---

## 🗑️ FILE OBSOLETI DA DEPRECARE

### 1. **ProbeListScreen.kt** (Post-Refactor Sonda Unica)

**File**: `app/src/main/java/com/app/miklink/ui/probe/ProbeListScreen.kt`

**Stato**: Schermata per gestire multi-sonda; post-refactor non più necessaria.

**Azione**:
- **OPZIONE A**: Eliminare completamente
- **OPZIONE B**: Mantenere ma rimuovere da `NavGraph.kt` (deprecata ma compatibile)

**Raccomandazione**: OPZIONE B (mantieni per backward compatibility temporanea).

**Modifica NavGraph**:
```kotlin
// RIMUOVI route "probe_list"
// composable("probe_list") { ProbeListScreen(navController) }
```

---

### 2. **ProbeListViewModel.kt**

**File**: `app/src/main/java/com/app/miklink/ui/probe/ProbeListViewModel.kt`

**Stato**: ViewModel per ProbeListScreen; deprecato con schermata.

**Azione**: MANTIENI per compatibilità, ma non referenziare in nuovo codice.

---

### 3. **ProbeViewModel.kt**

**File**: `app/src/main/java/com/app/miklink/ui/probe/ProbeViewModel.kt`

**Contenuto**:
```kotlin
@HiltViewModel
class ProbeViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {
    val allProbes: StateFlow<List<ProbeConfig>> = repository.probeConfigDao.getAllProbes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    fun saveProbe(probe: ProbeConfig) { ... }
    fun deleteProbe(probe: ProbeConfig) { ... }
    suspend fun checkAndVerifyProbe(probe: ProbeConfig) = repository.checkProbeConnection(probe)
}
```

**Problema**: Funzionalità coperte da `ProbeEditViewModel` e `DashboardViewModel`.

**Azione**: ELIMINA (non usato).

---

## 🧹 FUNZIONI INUTILIZZATE

### In `TestViewModel.kt`

**Funzione**: `resetState()`
```kotlin
fun resetState() {
    _log.value = emptyList()
    _uiState.value = UiState.Idle
    _sections.value = emptyList()
    _isRunning.value = false
    overrideClientNetwork = null
}
```

**Stato**: Definita ma MAI chiamata.

**Azione**:
- **OPZIONE A**: Eliminare
- **OPZIONE B**: Collegare a pulsante "Annulla" in UI

**Raccomandazione**: OPZIONE B (utile per reset senza pop back).

---

### In `ClientDao.kt`

**Funzione**: `updateNextIdAndStickyFields()` (rimossa in code review 2025-01-15)

**Stato**: GIÀ RIMOSSA (sostituita da `incrementNextIdNumber()`).

**Azione**: ✅ Completata.

---

### In `BackupRepository.kt`

**Funzioni**: `exportConfigToJson()`, `importConfigFromJson()`

**Stato**: Definite ma NON esposte in UI.

**Azione**: MANTIENI per futuro feature "Backup/Restore Config".

---

## 📦 IMPORT INUTILIZZATI

### In `TestExecutionScreen.kt`

**Imports non usati**:
```kotlin
import androidx.compose.animation.AnimatedVisibility // usato solo in commented code
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
```

**Azione**: IDE cleanup automatico (non critico).

---

### In `AppRepository.kt`

**Import**: Nessuno rilevato (file pulito).

---

## 🔄 FUNZIONI DUPLICATE (Same Logic)

### `observeProbeStatus()` vs `observeAllProbesWithStatus()`

**In AppRepository**:
```kotlin
fun observeProbeStatus(probe: ProbeConfig): Flow<Boolean> = flow {
    while (true) {
        val isOnline = try {
            val api = buildServiceFor(probe)
            api.getSystemResource(ProplistRequest(listOf("board-name"))).isNotEmpty()
        } catch (e: Exception) { false }
        emit(isOnline)
        delay(15000)
    }
}

fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>> = 
    probeConfigDao.getAllProbes().flatMapLatest { probes ->
        if (probes.isEmpty()) return@flatMapLatest flowOf(emptyList())
        tickerFlow(10_000L).map {
            withContext(Dispatchers.IO) {
                probes.map { probe ->
                    val isOnline = try {
                        val api = buildServiceFor(probe)
                        api.getSystemResource(...).isNotEmpty()
                    } catch (e: Exception) { false }
                    ProbeStatusInfo(probe, isOnline)
                }
            }
        }
    }
```

**Problema**: Logica polling duplicata (15s vs 10s interval).

**Azione**: POST-REFACTOR sonda unica, `observeAllProbesWithStatus()` deprecato.

**Raccomandazione**: ELIMINA `observeAllProbesWithStatus()` e `tickerFlow()` dopo refactor.

---

## 🎯 CLEANUP PLAN

### Fase 1: Eliminazione Duplicati Espliciti
```powershell
# Esegui in PowerShell
Remove-Item "C:\Users\dot\AndroidStudioProjects\MikLink\app\src\main\java\com\app\miklink\util\Compatibility.kt"
Remove-Item "C:\Users\dot\AndroidStudioProjects\MikLink\app\src\main\java\com\app\miklink\ui\SettingsScreen.kt"
Remove-Item "C:\Users\dot\AndroidStudioProjects\MikLink\app\src\main\java\com\app\miklink\data\network\dto\MikroTikDto.kt"
Remove-Item "C:\Users\dot\AndroidStudioProjects\MikLink\app\src\main\java\com\app\miklink\ui\probe\ProbeViewModel.kt"
```

### Fase 2: Refactor DTO Consolidation

**Azione**: Spostare DTO da `MikroTikApiService.kt` a nuovo file `data/network/dto/MikroTikDto.kt` (dopo eliminazione vecchio).

**Nuovo file**:
```kotlin
// MikroTikDto.kt
package com.app.miklink.data.network.dto

import com.squareup.moshi.Json

// System Resource
data class ProplistRequest(@Json(name = ".proplist") val proplist: List<String>)
data class SystemResource(@Json(name = "board-name") val boardName: String)

// Interface
data class InterfaceNameRequest(@Json(name = "?interface") val interfaceName: String)
data class NumbersRequest(@Json(name = "numbers") val numbers: String)
data class EthernetInterface(val name: String)

// DHCP Client
data class DhcpClientStatus(
    @Json(name = ".id") val id: String? = null,
    val disabled: String? = null,
    val status: String? = null,
    val address: String? = null,
    val gateway: String? = null,
    val dns: String? = null
)
data class DhcpClientAdd(
    @Json(name = "interface") val `interface`: String,
    @Json(name = "use-peer-dns") val usePeerDns: String = "yes"
)

// IP Address
data class IpAddressEntry(
    @Json(name = ".id") val id: String? = null,
    val address: String? = null,
    @Json(name = "interface") val iface: String? = null
)
data class IpAddressAdd(
    @Json(name = "address") val address: String,
    @Json(name = "interface") val `interface`: String
)

// Routes
data class RouteEntry(
    @Json(name = ".id") val id: String? = null,
    @Json(name = "dst-address") val dstAddress: String? = null,
    val gateway: String? = null
)
data class RouteAdd(
    @Json(name = "dst-address") val dstAddress: String,
    val gateway: String
)

// Cable Test
data class CableTestRequest(@Json(name = "numbers") val numbers: String)
data class CableTestResult(
    @Json(name = "cable-pairs") val cablePairs: List<Map<String, String>>,
    val status: String
)

// Monitor
data class MonitorRequest(
    @Json(name = "numbers") val numbers: String,
    @Json(name = "once") val once: Boolean = true
)
data class MonitorResponse(val status: String, val rate: String?)

// Neighbors
data class NeighborRequest(
    @Json(name = "?.query") val query: List<String>,
    @Json(name = ".proplist") val proplist: List<String>
)
data class NeighborDetail(
    val identity: String?,
    @Json(name = "interface-name") val interfaceName: String?,
    @Json(name = "system-caps-enabled") val systemCaps: String?,
    @Json(name = "discovered-by") val discoveredBy: String?,
    @Json(name = "vlan-id") val vlanId: String? = null,
    @Json(name = "voice-vlan-id") val voiceVlanId: String? = null,
    @Json(name = "poe-class") val poeClass: String? = null
)

// Ping
data class PingRequest(val address: String, val count: String = "4")
data class PingResult(@Json(name = "avg-rtt") val avgRtt: String?)

// Traceroute
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

**Poi aggiornare** `MikroTikApiService.kt`:
```kotlin
import com.app.miklink.data.network.dto.*

interface MikroTikApiService {
    @POST("/rest/system/resource/print")
    suspend fun getSystemResource(@Body request: ProplistRequest): List<SystemResource>
    
    // ...rest of methods...
}
```

### Fase 3: Deprecazione UI Sonda Multi

**In NavGraph.kt**:
```kotlin
// RIMUOVI o COMMENTA:
// composable("probe_list") { ProbeListScreen(navController) }
```

**In DashboardScreen.kt** (già coperto da MASTER_PLAN):
- Rimuovere card "Seleziona Sonda"

### Fase 4: Cleanup AppRepository Post-Refactor

**Rimuovere**:
```kotlin
// DEPRECATO: multi-probe support
fun observeAllProbesWithStatus(): Flow<List<ProbeStatusInfo>> { ... }
private fun tickerFlow(periodMs: Long): Flow<Unit> { ... }
data class ProbeStatusInfo(...) // spostare in file separato se serve
```

---

## 📊 IMPATTO CLEANUP

### Linee di Codice Rimosse
| File | Linee | Note |
|------|-------|------|
| `util/Compatibility.kt` | ~5 | File vuoto |
| `ui/SettingsScreen.kt` | ~5 | File vuoto |
| `data/network/dto/MikroTikDto.kt` | ~10 | Vuoto/obsoleto |
| `ui/probe/ProbeViewModel.kt` | ~35 | Non usato |
| `AppRepository` (observeAll...) | ~40 | Deprecato |
| **Totale** | **~95** | Codice morto |

### Linee di Codice Refactored
| File | Linee | Note |
|------|-------|------|
| `MikroTikDto.kt` (nuovo) | +120 | Consolidation DTO |
| `MikroTikApiService.kt` | -80 | DTO spostati |
| **Net Change** | **+40** | Più organizzato |

---

## ✅ CHECKLIST CLEANUP

### Eliminazione Files
- [ ] `util/Compatibility.kt`
- [ ] `ui/SettingsScreen.kt`
- [ ] `ui/probe/ProbeViewModel.kt`
- [ ] `data/network/dto/MikroTikDto.kt` (vecchio vuoto)

### Refactor DTO
- [ ] Creare nuovo `data/network/dto/MikroTikDto.kt` con tutti i DTO
- [ ] Aggiornare import in `MikroTikApiService.kt`
- [ ] Aggiornare import in `AppRepository.kt`
- [ ] Verificare nessun breaking change

### Deprecazione UI
- [ ] Rimuovere route "probe_list" da `NavGraph.kt`
- [ ] Commentare `ProbeListScreen` e `ProbeListViewModel` (non eliminare per compatibility)

### Cleanup AppRepository
- [ ] Rimuovere `observeAllProbesWithStatus()`
- [ ] Rimuovere `tickerFlow()`
- [ ] Rimuovere `data class ProbeStatusInfo` (o spostare)

### IDE Cleanup
- [ ] Eseguire "Optimize Imports" su tutti i file modificati
- [ ] Eseguire "Code Cleanup" Android Studio
- [ ] Verificare nessun warning "Unused" critico

---

## 🎯 POST-CLEANUP VALIDATION

### Build Check
```powershell
cd C:\Users\dot\AndroidStudioProjects\MikLink
.\gradlew clean build
```

**Expected**: Build SUCCESS senza warning critici.

### Lint Check
```powershell
.\gradlew lint
```

**Expected**: Nessun nuovo error, solo info/warning minori.

### Test Manuali
- [ ] Dashboard apre correttamente
- [ ] Settings apre correttamente
- [ ] Test esecuzione funziona (nessun crash)
- [ ] Nessun import error in IDE

---

**Fine DUPLICATES_CLEANUP.md**

