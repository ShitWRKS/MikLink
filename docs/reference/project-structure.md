# Project structure (Canone A)

Questa pagina è **normativa**: descrive la struttura ammessa e le regole di import/naming.

> Se una cartella/file non rispetta il canone, va migrata o rimossa (nessuna eccezione senza ADR).

## Struttura canonica

- `app/src/main/java/com/app/miklink/`
  - `core/`
  - `domain/`
  - `model/`
  - `usecase/`
  - `...`
  - `data/`
    - `# SOLO ports/contratti + tipi neutrali`
  - `repository/`
  - `gateway/`
  - `provider/`
  - `io/`
  - `pdf/`
  - `...`
  - `data/`
    - `# Implementazioni infra + mapper`
    - `local/`  `# Room / DataStore / file`
    - `remote/` `# Retrofit/OkHttp/Moshi`
    - `report/` `# codec/mapper report (infra)`
    - `repositoryimpl/` `# implementazioni dei port`
    - `...`
  - `ui/`
    - `# Compose + ViewModel + UiState + mapper domain->ui`
  - `di/`
    - `# Wiring DI (Hilt)`
  - `utils/`
    - `# helper “non di dominio” (no dipendenze ui<->data)`

markdown
Copia codice

## Regole di responsabilità

- `core/domain/**`: puro business.
  - Vietati: Android SDK, Room, Retrofit/OkHttp, Moshi, iText/PDF, filesystem/SAF.
- `core/data/**`: solo **contratti** e **tipi neutrali** (ports, DTO neutrali, destinazioni IO neutrali, ecc.).
  - Vietate implementazioni tecnologiche.
- `data/**`: implementazioni tecnologiche (Room, Retrofit/Moshi, OkHttp, PDF, SAF/ContentResolver, DataStore, ecc.).
- `ui/**`: solo presentazione.
  - Vietato importare `data/**` (usa use case e ports del core).
- `di/**`: solo wiring.

## Cartelle non canoniche

- `feature/**` → da eliminare/migrare (non usare per nuove feature)
- `domain/**` top-level → da migrare in `core/domain/**`
- qualunque cartella con suffissi `v1/`, `v2/` → vietata (usa Room schemaVersion, non naming)

## Vincoli di naming (hard)

- `probeId` è un concetto legacy e **vietato** come naming/termine pubblico.
  - Hard rule: `git grep -n "probeId" app/src/main app/src/test` deve tornare **0 risultati** (anche commenti).

## Checklist di revisione rapida

- Un file in `core/domain/**` importa `android.*`? → **errore**
- Un file in `core/domain/**` importa `retrofit2.*` / `okhttp3.*` / `com.squareup.moshi.*` / `androidx.room.*`? → **errore**
- Un file in `core/data/**` contiene implementazioni concrete (Room/Retrofit/iText/SAF)? → **errore**
- Un file in `ui/**` importa `androidx.room` / `dao` / `entity` / `data/**`? → **errore**
- Un file in `data/**` importa `ui/**`? → **errore**

## Annotation processing

- Room usa KSP (`room-compiler`).
- Hilt usa KSP (`hilt-compiler`).
- KAPT non è usato.