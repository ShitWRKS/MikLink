# MikLink

**MikLink** è un'applicazione Android nativa per il testing e la certificazione di infrastrutture di rete basate su MikroTik. Progettata per tecnici di campo, consente di eseguire test diagnostici (TDR, Link Status, LLDP, Ping) su dispositivi client tramite una sonda MikroTik configurabile, generando report PDF di certificazione.

---

## 📋 Architettura

### Pattern Architetturale
- **MVVM (Model-View-ViewModel)** con **Dependency Injection (Hilt)**
- **Unidirectional Data Flow**: State Hoisting con Jetpack Compose
- **Reattività**: Coroutine (`viewModelScope`, `Dispatchers.IO`) e Kotlin Flow

### Stack Tecnologico

#### UI Layer
- **Jetpack Compose** (Material 3)
- **Navigation Compose** (`NavHost` in `NavGraph.kt`)
- **Hilt Navigation Compose** per l'iniezione ViewModel

#### Data Layer
- **Room Database** (local persistence)
  - DAO pattern per query SQL type-safe
  - Accesso asincrono via Coroutine
- **Retrofit** + **Moshi** (networking)
  - Comunicazione REST con MikroTik API
  - `AuthInterceptor` per autenticazione custom
  - `OkHttp` Logging Interceptor per debug

#### Dependency Injection
- **Hilt** (Dagger 2)
  - Moduli: `DatabaseModule`, `NetworkModule`
  - `@HiltAndroidApp` in `MikLinkApplication`
  - `@HiltViewModel` per tutti i ViewModel

---

## 🗂️ Schema Database (Room)

### Entità Principali

#### **Client** (`clients`)
Rappresenta un'azienda/location da testare.
- Campi: `companyName`, `location`, `networkMode` (DHCP/Static), `staticCidr`, `minLinkRate` (soglia di validità), `socketPrefix`, `nextIdNumber`

#### **ProbeConfig** (`probe_config`)
Configurazione della sonda MikroTik (dispositivo di test).
- Campi: `name`, `ipAddress`, `username`, `password`, `testInterface`, `isOnline`, `modelName`, `tdrSupported`, `isHttps`

#### **TestProfile** (`test_profiles`)
Profilo di test configurabile (quali test eseguire).
- Campi: `profileName`, `runTdr`, `runLinkStatus`, `runLldp`, `runPing`, `pingTarget1/2/3`, `pingCount`

#### **Report** (`test_reports`)
Storico dei report generati.
- Campi: `timestamp`, `clientId`, `socketName`, `notes`, `probeName`, `profileName`, `overallStatus`, `resultsJson` (payload JSON dei risultati)

---

## 🧭 Navigazione

Il grafo di navigazione è definito in `NavGraph.kt` con Jetpack Compose Navigation:

### Schermate Principali
- **Dashboard** (`dashboard`) - Schermata iniziale per avvio test e accesso alle sezioni
- **Test Execution** (`test_execution/{clientId}/{probeId}/{profileId}/{socketName}`) - Esecuzione test in tempo reale
- **History** (`history`) - Elenco report storici
- **Report Detail** (`report_detail/{reportId}`) - Dettaglio singolo report

### Schermate di Gestione (CRUD)
- **Client Management** - Lista (`client_list`), Aggiungi/Modifica (`client_add`, `client_edit/{clientId}`)
- **Probe Management** - Aggiungi/Modifica (`probe_add`, `probe_edit/{probeId}`)
- **Profile Management** - Lista (`profile_list`), Aggiungi/Modifica (`profile_add`, `profile_edit/{profileId}`)
- **Settings** (`settings`) - Configurazioni app

---

## 🛠️ ViewModel Layer

Tutti i ViewModel usano `@HiltViewModel` e seguono il pattern di gestione dello stato con `StateFlow`/`MutableStateFlow`:

- **DashboardViewModel** - Gestione stato dashboard e caricamento dati iniziali
- **TestViewModel** - Orchestrazione esecuzione test (chiamate API sequenziali/parallele)
- **HistoryViewModel** - Query report dal database
- **ReportDetailViewModel** - Deserializzazione e visualizzazione dettagli report
- **ClientListViewModel** / **ClientEditViewModel** - CRUD clienti
- **ProbeListViewModel** / **ProbeEditViewModel** - CRUD sonde
- **TestProfileViewModel** - CRUD profili di test
- **SettingsViewModel** - Configurazioni applicazione

---

## 🌐 Networking

### API MikroTik
- **Service**: `MikroTikApiService` (interfaccia Retrofit)
- **Autenticazione**: `AuthInterceptor` (gestione credenziali custom)
- **DTOs**: Package `data.network.dto` per serializzazione/deserializzazione JSON (Moshi)

### Test Diagnostici Implementati
1. **TDR (Time Domain Reflectometer)** - Analisi cavo (lunghezza, eventuali guasti)
2. **Link Status** - Stato link (speed, duplex, auto-negotiation)
3. **LLDP (Link Layer Discovery Protocol)** - Discovery switch/dispositivi connessi
4. **Ping** - Test connettività verso target configurabili

---

## 📦 Build System

- **Gradle** (Kotlin DSL)
- **Java/Kotlin Toolchain**: JVM 21
- **KSP (Kotlin Symbol Processing)** per Room e Hilt
- **Version Catalog**: Gestione dipendenze centralizzata (`libs.versions.toml`)

### Dipendenze Principali
```toml
Kotlin: 2.0.0
Compose BOM: 2024.06.00
Hilt: 2.51.1
Room: 2.6.1
Retrofit: 2.9.0
Navigation Compose: 2.7.7
```

---

## 🚀 Getting Started

### Prerequisiti
- Android Studio Ladybug (2024.2.1+)
- JDK 21
- Dispositivo Android (minSdk 26, targetSdk 34) o Emulator

### Build e Run
```powershell
# Build da terminale
.\gradlew clean build

# Installazione su dispositivo
.\gradlew installDebug
```

### Configurazione Sonda
1. Avviare l'app
2. Navigare in **Settings** → **Probe Configuration**
3. Inserire: IP, username, password, interfaccia di test
4. Verificare connessione con "Test Connection"

### Primo Test
1. Creare un **Client** (Dashboard → Manage Clients)
2. Creare un **Test Profile** (Dashboard → Manage Profiles)
3. Dalla Dashboard, selezionare Client, Profilo e avviare il test

---

## 📄 Struttura del Progetto

```
app/src/main/java/com/app/miklink/
├── data/
│   ├── db/
│   │   └── model/          # Room Entities
│   ├── network/
│   │   ├── dto/            # Network DTOs
│   │   ├── MikroTikApiService.kt
│   │   └── AuthInterceptor.kt
│   ├── repository/         # Repository pattern (data access layer)
│   └── pdf/                # PDF generation logic
├── di/                     # Hilt Modules (Database, Network)
├── ui/
│   ├── client/             # Client Management Screens
│   ├── dashboard/          # Dashboard Screen
│   ├── history/            # Report History Screens
│   ├── probe/              # Probe Configuration Screens
│   ├── profile/            # Test Profile Screens
│   ├── settings/           # Settings Screen
│   ├── test/               # Test Execution Screen
│   ├── theme/              # Compose Theme
│   └── NavGraph.kt         # Navigation Graph
├── util/                   # Utility classes
└── MikLinkApplication.kt   # Application class (@HiltAndroidApp)
```

---

## 📝 Note Tecniche

- **Thread Safety**: Tutte le operazioni DB/Network vengono eseguite su `Dispatchers.IO`
- **State Management**: Pattern State Hoisting per Composable stateless
- **Error Handling**: Try-catch su operazioni async con propagazione stato error via UiState
- **PDF Generation**: Report esportabili in PDF con logo/branding custom

---

## 📚 Documentazione

La documentazione tecnica dettagliata, lo schema dell'architettura e il design system sono disponibili nella nostra cartella /docs.

- **[Indice Principale Documentazione](docs/README.md)**
- **[Architettura Completa](docs/ARCHITECTURE.md)**
- **[Design System UI/UX](docs/UX_UI_SPEC.md)**
- **[Checklist Test API](docs/API_VALIDATION.md)**

---

## 🔧 Maintenance

### Testing
- Unit Tests: `src/test/` (JUnit 4)
- Instrumentation Tests: `src/androidTest/` (Espresso, Compose Testing)
- **API Testing**: Seguire checklist in [docs/API_VALIDATION.md](docs/API_VALIDATION.md)

### Database Migrations
Le migrazioni Room sono gestite manualmente. Incrementare `version` in `@Database` e fornire `Migration` object al builder.

---

**Versione App**: 1.0  
**Target SDK**: Android 14 (API 34)  
**Min SDK**: Android 8.0 Oreo (API 26)

