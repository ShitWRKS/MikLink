# DISCREPANCIES (Docs vs Codice)

Questo file contiene **solo evidenze** tra docs e codice.  
Non contiene proposte o soluzioni (quelle vanno in epic/PR).

## Regole

- Una discrepanza = un blocco con:
  - data
  - evidenza (path + simboli)
  - perché è una discrepanza (rispetto a quale doc/ADR)

---

## Discrepanze note (da risolvere nel refactor)

### D-001 — `probeId` ancora presente nel flow

- **Data:** 2025-12-14
- **Docs:** ADR-0001, ADR-0003
- **Evidenze (esempi):**
  - `app/src/main/java/com/app/miklink/ui/NavGraph.kt` route `test_execution/{clientId}/{probeId}/...`
  - `app/src/main/java/com/app/miklink/core/domain/test/model/TestPlan.kt` contiene `probeId`
  - `app/src/main/java/com/app/miklink/core/data/local/room/v1/model/ProbeConfig.kt` PK `probeId`

### D-002 — HTTPS trust-all applicato globalmente

- **Data:** 2025-12-14
- **Docs:** ADR-0002
- **Evidenze (esempi):**
  - `app/src/main/java/com/app/miklink/di/NetworkModule.kt` configura SSL trust-all e hostnameVerifier senza gating su `isHttps`

### D-003 — PDF dipende da UI model

- **Data:** 2025-12-14
- **Docs:** `explanation/architecture.md` (Results canonical)
- **Evidenze (esempi):**
  - `app/src/main/java/com/app/miklink/core/data/pdf/impl/PdfGeneratorIText.kt` importa `com.app.miklink.ui.history.model.ParsedResults`
  - `app/src/main/java/com/app/miklink/core/data/pdf/parser/ParsedResultsParser.kt` ritorna `ParsedResults`

### D-004 — UI dipende da DTO remoti per i risultati

- **Data:** 2025-12-14
- **Docs:** `explanation/architecture.md` (Results canonical)
- **Evidenze (esempi):**
  - `app/src/main/java/com/app/miklink/ui/history/model/ParsedResults.kt` importa DTO MikroTik

### D-005 — DB “v1/v2” e migrazioni ancora presenti

- **Data:** 2025-12-14
- **Docs:** ADR-0003, `reference/database.md`
- **Evidenze (esempi):**
  - `core.data.local.room.v1.AppDatabase` con version 13
  - `Migrations.kt` e `MigrationTest.kt`
  - nome DB file `miklink-db`

### D-006 — Cartelle non canoniche presenti

- **Data:** 2025-12-14
- **Docs:** `reference/project-structure.md`
- **Evidenze (esempi):**
  - `feature/**` presente
  - `domain/**` top-level presente
  - `data/repositoryimpl/roomv1/**` presente

