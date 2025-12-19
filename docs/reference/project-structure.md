# Struttura progetto e convenzioni

Questa pagina è la **reference** per:
- struttura cartelle/package (dove mettere cosa)
- nomenclatura file/classi
- regole di dipendenza tra layer

Fonte di verità: package nel codice sotto `app/src/main/java/com/app/miklink/**`.

## Regole di dipendenza (non negoziabili)

- `core/domain/**` **NON** importa:
  - `android.*`, `androidx.*`
  - `com.app.miklink.data.*`, `com.app.miklink.ui.*`, `com.app.miklink.di.*`
- `core/data/**` (porte/contract) **NON** importa `com.app.miklink.data.*`
  - in particolare: **mai DTO Retrofit/Moshi dentro le porte**
- `data/**` può importare `core/**` e framework (Room, Retrofit/Moshi, iText, Android I/O)
- `ui/**` importa `core/**` e usa use case/porte, non implementazioni concrete
- `di/**` può importare tutto (wiring), ma non deve contenere logica di business

## Albero target (package)

> Nota: i nomi “macro” restano quelli attuali (`core/domain`, `core/data`, `data`, `ui`, `di`).
> Questa struttura serve per evitare package ambigui (`repositoryimpl`, `utils` onnivori) e per rendere i confini SOLID verificabili.

```text
com.app.miklink
  core/
    domain/
      model/                      # entità/value object: Client, ProbeConfig, TestReport, TestProfile e simili
      policy/                     # regole business pure (validazioni, socket-id, ecc.)
      test/                       # modelli e astrazioni del runner (step/result/outcome)
      usecase/                    # orchestrazione applicativa: salva report (SaveTestReportUseCase), export/import, ecc.
    data/
      repository/                 # PORTE: interfacce repository per feature
        client/
        probe/
          model/                  # modelli di confine (no DTO): ProbeCheckResult, ProbeStatusInfo e simili
        report/
        test/
        testprofile/
        preferences/
        backup/
      io/                         # DocumentReader/Writer (contract)
      pdf/                        # PdfGenerator + config (contract)
      report/                     # codec/serializer contract (es. ReportResultsCodec)
  data/
    local/
      room/
        db/                       # MikLinkDatabase
        dao/
        entity/
        mapper/                   # toDomain/toEntity
        transaction/              # runner transazioni
      datastore/                  # implementazioni prefs
    remote/
      mikrotik/
        api/                      # Retrofit service
        dto/                      # DTO Moshi/Retrofit (restano qui)
        mapper/                   # dto -> domain
        infra/                    # OkHttp, SSL policy, interceptors, Moshi adapters
        provider/                 # provider network binding (Android)
        service/                  # MikroTikCallExecutor (unico path di chiamata, riusa factory/provider)
    repository/
      room/                       # RoomClientRepository, RoomReportRepository e simili
      mikrotik/                   # MikroTik*Repository (per esempio MikroTikProbeStatusRepository) senza suffisso Impl
      backup/                     # DefaultBackupRepository / BackupManagerImpl
      common/                     # componenti di composizione (es. RouteManager)
    pdf/
      itext/                      # implementazione iText
    io/
      android/                    # implementazioni Android document I/O
  ui/
    navigation/
    theme/
    components/                   # composables riusabili
    feature/                      # vertical slice per feature (consigliato)
      probe/
      test/
      test_details/               # renderer unificato live+history basato su TestRunSnapshot
      history/
      client/
      settings/
  di/
    module/
      DatabaseModule.kt
      NetworkModule.kt
      RepositoryBindingsModule.kt
      UseCaseBindingsModule.kt
      PdfModule.kt
      TestRunnerModule.kt
```

## Convenzioni di nomenclatura

### Porte e implementazioni

- Porta: `XxxRepository`
- Implementazione esplicita:
  - Room: `RoomXxxRepository`
  - MikroTik: `MikroTikXxxRepository`
  - DataStore: `DataStoreXxxRepository` (o `DataStoreUserPreferencesRepository`)
- Evita `XxxRepositoryImpl` generico: non comunica quale implementazione sia.

### Remote / DTO / mapping

- Retrofit: `MikroTikApiService` (ok)
- DTO: `XxxDto` in `data/remote/mikrotik/dto`
- Mapper: funzioni `toDomain()` / `toDto()` in `data/remote/mikrotik/mapper`
- Le porte `core/data/**` espongono solo tipi dominio o boundary model senza annotation framework.

### Room

- Entity: `XxxEntity`
- DAO: `XxxDao`
- Mapper: `toEntity()` / `toDomain()`

### UI (Compose)

Per ogni feature:
- `XxxScreen.kt` — solo UI
- `XxxViewModel.kt`
- `XxxUiState.kt` (e `XxxEvent.kt` se serve)
- `components/` locali alla feature se non riutilizzabili

## Guardrail anti-drift

Quando possibile, aggiungere (o mantenere) controlli automatici:
- static analysis (Detekt) per vietare import proibiti in `core/domain` e `core/data`
- test di compilazione/contract che falliscono se un confine viene violato
