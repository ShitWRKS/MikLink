# 🎯 PROMPT AGGIUNTIVI PER ANALISI AI SPECIFICHE

Questi prompt sono ottimizzati per ottenere analisi mirate da LLM (Claude, ChatGPT, Gemini) sul progetto MikLink.

---

## 📋 PREREQUISITI

Prima di usare questi prompt, assicurati di avere:

1. ✅ File `project_structure.txt` (generato con `tree /F /A`)
2. ✅ Questo report di analisi: `CODEBASE_ANALYSIS_REPORT.md`
3. ✅ (Opzionale) Output dell'ispezione di Android Studio

---

## 🔧 PROMPT 1: Refactoring ViewModel Duplications

```
Analizza i seguenti ViewModel del progetto MikLink e proponi un refactoring completo:

FILE 1 - ClientEditViewModel.kt:
[INCOLLA QUI IL CONTENUTO DI app/src/main/java/com/app/miklink/ui/client/ClientEditViewModel.kt]

FILE 2 - ProbeEditViewModel.kt:
[INCOLLA QUI IL CONTENUTO DI app/src/main/java/com/app/miklink/ui/probe/ProbeEditViewModel.kt]

FILE 3 - TestProfileViewModel.kt:
[INCOLLA QUI IL CONTENUTO DI app/src/main/java/com/app/miklink/ui/profile/TestProfileViewModel.kt]

COMPITO:
1. Identifica TUTTE le duplicazioni di codice tra questi tre ViewModel
2. Proponi una classe BaseEditViewModel<T> generica che elimini le duplicazioni
3. Mostra come refactorare ClientEditViewModel usando la nuova BaseEditViewModel
4. Considera:
   - SavedStateHandle management
   - StateFlow initialization pattern
   - Load/Save lifecycle
   - Error handling

FORMATO OUTPUT:
- Codice Kotlin completo per BaseEditViewModel
- Esempio completo di refactoring per almeno un ViewModel
- Lista di benefici quantificati (righe risparmiate, manutenibilità)
```

---

## 🗄️ PROMPT 2: Database Index Optimization

```
Analizza le seguenti Entity e DAO del database Room del progetto MikLink:

ENTITY - Report.kt:
[INCOLLA IL CONTENUTO DI app/src/main/java/com/app/miklink/data/db/model/Report.kt]

DAO - ReportDao.kt:
[INCOLLA IL CONTENUTO DI app/src/main/java/com/app/miklink/data/db/dao/ReportDao.kt]

QUERY PIÙ FREQUENTI:
- getReportsForClient(clientId) con ORDER BY timestamp DESC
- getLastReportForClient(clientId) con LIMIT 1
- getAllReports() con ORDER BY timestamp DESC

COMPITO:
1. Identifica quali query beneficerebbero di indici
2. Proponi la migliore strategia di indicizzazione
3. Considera:
   - Indici singoli vs compositi
   - Costo write vs guadagno read
   - Dimensione database prevista (~1000-10000 report)
4. Genera il codice Kotlin per:
   - Entity aggiornata con @Index
   - Migration necessaria
5. Stima il miglioramento percentuale delle performance

FORMATO OUTPUT:
- Codice completo per Entity con indices
- Migration Room completa
- Analisi costi/benefici
```

---

## 📦 PROMPT 3: Dependency Audit Completo

```
Analizza le dipendenze del progetto MikLink Android:

FILE - app/build.gradle.kts:
[INCOLLA TUTTO IL CONTENUTO DEL FILE]

FILE - gradle/libs.versions.toml:
[INCOLLA TUTTO IL CONTENUTO DEL FILE]

STRUTTURA PROGETTO:
- 10 ViewModels con Hilt
- Jetpack Compose UI (11 screens)
- Room database (4 DAOs, 5 entities)
- Retrofit API (MikroTikService)
- PDF generation
- Backup/restore features

COMPITO:
1. Per OGNI dipendenza in build.gradle.kts:
   - Verifica se è realmente utilizzata
   - Identifica dipendenze transitive duplicate
   - Suggerisci alternative più leggere se applicabile
2. Analisi specifica:
   - `androidx.compose.material:material-icons-extended` - stimare n° icone usate
   - `compose-ui-tooling` - dovrebbe essere solo debug?
   - Librerie di test: tutte necessarie?
3. Genera:
   - Lista dipendenze SICURE da rimuovere
   - Lista dipendenze PROBABILMENTE inutilizzate (servono test)
   - Versioni aggiornabili con breaking changes
   - Stima riduzione APK size

FORMATO OUTPUT:
- Tabella comparativa PRIMA/DOPO
- build.gradle.kts ottimizzato
- Rischi e benefici per ogni modifica
```

---

## 🔍 PROMPT 4: Error Handling Standardization

```
Analizza la gestione degli errori nel repository del progetto MikLink:

FILE - AppRepository.kt:
[INCOLLA app/src/main/java/com/app/miklink/data/repository/AppRepository.kt]

FUNZIONE ATTUALE:
```kotlin
private suspend fun <T> safeApiCall(apiCall: suspend () -> T): T? {
    return try {
        apiCall()
    } catch (e: Exception) {
        null
    }
}
```

PROBLEMI IDENTIFICATI:
- Ritorna null senza informazioni sull'errore
- Non tutti i metodi usano safeApiCall()
- Mancano errori specifici per domini (NetworkError, ValidationError, etc.)
- UI riceve null e deve indovinare cosa è andato storto

COMPITO:
1. Progetta una gerarchia di sealed class per error handling:
   - Result<T> (Success, Error)
   - MikLinkError con sottoclassi (NetworkError, ApiError, ValidationError, etc.)
2. Refactora safeApiCall() per ritornare Result<T>
3. Mostra il refactoring di almeno 3 metodi del repository
4. Crea estensioni utili: `.onSuccess {}`, `.onError {}`, `.getOrNull()`, etc.
5. Mostra come gestire questi Result nei ViewModel

FORMATO OUTPUT:
- Gerarchia completa di sealed classes
- safeApiCall() refactorato
- 3 esempi di metodi repository aggiornati
- ViewModel pattern per consumare Result<T>
- Migration guide: come aggiornare gradualmente il codice esistente
```

---

## 🧪 PROMPT 5: Testing Strategy e Coverage

```
Analizza la strategia di testing attuale del progetto MikLink:

TEST ESISTENTI (tutti in app/src/test/):
[LISTA I FILE IN app/src/test/java/com/app/miklink/]

ESEMPIO TEST VIEWMODEL:
[INCOLLA UN TEST ESISTENTE, es. ClientEditViewModelTest.kt]

DIPENDENZE TEST (da build.gradle.kts):
- JUnit 4.13.2
- MockK 1.13.12
- Coroutines-test
- Turbine (Flow testing)
- Robolectric
- Room-testing
- Espresso
- Compose UI test

COPERTURA ATTUALE STIMATA: ~60%

COMPITO:
1. Analizza i test esistenti e identifica pattern comuni
2. Proponi test template riutilizzabili
3. Identifica AREE NON TESTATE:
   - Repository integration tests
   - DAO edge cases
   - UI/Compose tests
   - Error handling paths
4. Proponi una strategia di testing completa:
   - Unit tests (target 90% coverage)
   - Integration tests (Repository + DAO)
   - UI tests (critical paths, es: Test Execution flow)
5. Codice esempio per:
   - BaseViewModelTest<T> class
   - Repository integration test con Room in-memory
   - Compose UI test per una screen

TARGET: Portare coverage da 60% a 85%

FORMATO OUTPUT:
- Analisi gap coverage
- Priority matrix: High/Medium/Low per area
- 3 template test completi
- Testing checklist per ogni nuova feature
- Comandi gradle per coverage reports
```

---

## 🏗️ PROMPT 6: Architectural Improvements

```
Analizza l'architettura complessiva del progetto MikLink:

STRUTTURA CORRENTE:
[INCOLLA L'OUTPUT DI: tree /F /A app/src/main/java/com/app/miklink]

KEY FILES:
- NavGraph.kt: [INCOLLA CONTENUTO]
- AppDatabase.kt: [INCOLLA CONTENUTO]
- NetworkModule.kt: [INCOLLA CONTENUTO]
- DatabaseModule.kt: [INCOLLA CONTENUTO]

PATTERN ATTUALI:
- MVVM con ViewModels + Compose
- Repository pattern (2 repositories)
- Room per persistenza
- Hilt per DI
- Single Activity + Jetpack Navigation

COMPITO:
1. Valuta l'aderenza ai principi SOLID
2. Identifica violazioni delle Clean Architecture layers
3. Analizza:
   - Feature organization: va bene per feature/ modules?
   - Dipendenze circolari?
   - Accoppiamento ViewModel <-> Repository
   - Mancanza di Use Cases / Interactors layer?
4. Proponi miglioramenti:
   - Package restructure (se necessario)
   - Introduzione di Use Cases
   - Domain layer separato
   - UI State management patterns

CONSIDERAZIONI:
- Progetto ha ~6000 LOC, non vogliamo over-engineering
- Ma deve scalare a 50+ screens in futuro
- Team di 2-3 developers

FORMATO OUTPUT:
- Diagramma architettura ATTUALE (in Mermaid)
- Diagramma architettura PROPOSTA (in Mermaid)
- Migration path incrementale (5 fasi)
- Trade-offs: complessità vs benefici
```

---

## 🎨 PROMPT 7: StateFlow vs UiState Refactoring

```
Analizza il pattern di gestione stato UI nei ViewModel del progetto MikLink:

ESEMPIO 1 - TestViewModel (COMPLESSO):
[INCOLLA app/src/main/java/com/app/miklink/ui/test/TestViewModel.kt]

ESEMPIO 2 - ProbeEditViewModel (MEDIO):
[INCOLLA app/src/main/java/com/app/miklink/ui/probe/ProbeEditViewModel.kt]

PROBLEMI IDENTIFICATI:
- 10+ StateFlow separati in TestViewModel
- Difficile tracciare lo stato completo della UI
- Impossibile fare snapshot dello stato per debug
- Problemi di race condition potenziali

COMPITO:
1. Proponi un pattern UiState unificato:
   ```kotlin
   data class TestExecutionUiState(
       val isRunning: Boolean,
       val sections: List<TestSection>,
       val log: List<String>,
       // ...
   )
   ```
2. Mostra il refactoring completo di TestViewModel:
   - Da 10+ MutableStateFlow a un singolo StateFlow<UiState>
   - Come gestire updates parziali (copy() pattern)
   - Loading/Error/Success states
3. Vantaggi:
   - Atomic state updates
   - Easy state debugging
   - Time-travel debugging ready
   - Testability migliorata
4. Mostra come il Composable consumer cambia:
   - Prima: 10+ state collections
   - Dopo: 1 state collection

FORMATO OUTPUT:
- UiState sealed class hierarchy
- TestViewModel refactorato
- UI Compose aggiornata
- Before/After comparison
- Testing comparison (con/senza UiState)
```

---

## 🚀 PROMPT 8: Performance Profiling Guidelines

```
Crea una guida completa per il profiling delle performance del progetto MikLink.

CONTESTO:
- App Android con Jetpack Compose
- Database Room con potenzialmente migliaia di Report
- Network calls a dispositivi MikroTik via API REST
- Generazione PDF in-app

AREE CRITICHE DA PROFILARE:
1. Room queries performance (Report filtering)
2. Compose recomposition (TestExecutionScreen ha molti state)
3. PDF generation memory usage
4. Network timeout e retry logic
5. App startup time

COMPITO:
1. Crea una checklist di profiling completa
2. Per ogni area critica:
   - Tool da usare (Android Profiler, Macrobenchmark, etc.)
   - Metriche chiave da misurare
   - Threshold accettabili
3. Codice esempio:
   - Room query con EXPLAIN QUERY PLAN
   - Compose recomposition tracking
   - Memory leak detection setup
4. Proponi optimizations comuni:
   - LazyColumn best practices
   - Paging3 per Report infiniti?
   - WorkManager per PDF async?
   - Image/icon loading ottimizzato

FORMATO OUTPUT:
- Profiling checklist markdown
- Script gradle per benchmarking
- Performance test examples
- Optimization priority matrix
```

---

## 📚 COME USARE QUESTI PROMPT

### Workflow Consigliato:

1. **Setup Iniziale**:
   ```bash
   # Genera structure tree
   tree /F /A > project_structure.txt
   
   # Leggi il report principale
   cat CODEBASE_ANALYSIS_REPORT.md
   ```

2. **Scegli il Prompt** in base alla priorità dal report principale

3. **Prepara i File**:
   - Copia i contenuti dei file richiesti dal prompt
   - Usa `cat file.kt` su Linux/Mac o `Get-Content file.kt` su PowerShell

4. **Esegui il Prompt** su Claude/ChatGPT/Gemini:
   - Incolla il prompt completo
   - Aggiungi i contenuti dei file richiesti
   - Chiedi follow-up specifici

5. **Valida l'Output**:
   - Crea un branch: `git checkout -b refactor/prompt-X-description`
   - Implementa le modifiche suggerite
   - Testa con: `./gradlew test`
   - Code review prima di merge

### Priorità Suggerita:

1. **PROMPT 1** (ViewModel Refactoring) - Impatto immediato
2. **PROMPT 2** (Database Indices) - Performance boost
3. **PROMPT 4** (Error Handling) - Qualità codice
4. **PROMPT 3** (Dependencies) - APK size
5. **PROMPT 7** (UiState) - Complessità gestione stato
6. **PROMPT 5** (Testing) - Coverage
7. **PROMPT 6** (Architecture) - Scalabilità long-term
8. **PROMPT 8** (Performance) - Ottimizzazioni finali

---

## 🔗 Files Utili da Avere a Portata:

```bash
# Crea una cartella con tutti i file che servono per i prompt
mkdir analysis_workspace
cp app/build.gradle.kts analysis_workspace/
cp gradle/libs.versions.toml analysis_workspace/
cp -r app/src/main/java/com/app/miklink/ui/ analysis_workspace/ui/
cp -r app/src/main/java/com/app/miklink/data/ analysis_workspace/data/
cp CODEBASE_ANALYSIS_REPORT.md analysis_workspace/
cp project_structure.txt analysis_workspace/
```

---

**Ultimo aggiornamento**: 2025-11-20  
**Compatibile con**: Claude 3.5, GPT-4, Gemini 2.0  
**Progetto**: MikLink v1.0
