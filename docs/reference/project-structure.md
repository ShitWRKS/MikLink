# Project structure (canone)

Questa pagina è **normativa**: descrive la struttura ammessa e le regole di naming/import.

> Se una cartella/file non rispetta il canone, va migrata o rimossa (nessuna eccezione senza ADR).

## Struttura canonica

```
app/src/main/java/com/app/miklink/
  core/
    domain/
      model/
      usecase/
      ...
    data/
      # SOLO ports/contratti
      repository/
      gateway/
      provider/
      ...
  data/
    # Implementazioni infra + mapper
    db/
    remote/
    repositoryimpl/
    parser/
    ...
  ui/
    # Compose + ViewModel + UiState + mapper domain->ui
  di/
    # Wiring DI
```

## Cartelle non canoniche

- `feature/**` → da eliminare/migrare (non usare per nuove feature)
- `domain/**` top-level → da migrare in `core/domain/**`
- qualunque cartella con suffissi `v1/`, `v2/` → vietata (usa Room schemaVersion, non naming)

## Regole di responsabilità

- `core/domain/**`: puro business. Niente Android, niente rete, niente DB.
- `core/data/**`: solo interfacce/ports. Niente implementazioni.
- `data/**`: DB/rete/PDF + mapper/parsing.
- `ui/**`: solo presentazione.
- `di/**`: solo wiring.

## Checklist di revisione rapida

<<<<<<< HEAD
- Un file in `core/domain/**` ha import `android.*`? → **errore**
- Un file in `ui/**` importa `room`/`dao`/`entity`? → **errore**
- Un file in `data/**` importa `ui/**`? → **errore**
- Esiste `probeId` in qualsiasi file Kotlin? → **errore**
=======
## Cartelle presenti ma “in migrazione”

- `legacy/`  
- `domain/` (top-level, fuori dal canone `core/domain/**`)
- `feature/` (struttura parallela a `ui/`)

**Regola pratica:** nessun nuovo codice dovrebbe nascere qui; si migra verso i layer canonici e poi si elimina.

## Convenzioni

- **Domain models**: in `core/domain/**/model`
- **Use cases**: in `core/domain/usecase/**`
- **Repository interfaces**: in `core/data/repository/**`
- **Repository implementations**: in `data/repositoryimpl/**`
- **Room**: è considerata infrastruttura ⇒ deve vivere in `data/**` (target).

## Regole di import (Canone A)

- `core/domain/**` è puro: può importare solo Kotlin stdlib, modelli dominio e i port definiti in `core/data/**`; vietato Android SDK, Room/Retrofit/OkHttp, Moshi, iText.
- `core/data/**` espone solo contratti e tipi neutri; può dipendere da modelli di dominio ma non da infrastruttura (Android, Room, Retrofit/Moshi, iText, SAF, ContentResolver).
- `data/**` contiene le impl tecnologiche; può importare Android/Room/Retrofit/Moshi/iText ma non deve essere importato da UI o domain e non deve esporre tipi infra nelle firme pubbliche.
- `ui/**` usa soltanto use case e modelli dominio; nessun import di `data/**` o di DTO/infra.
- I use case possono dipendere dai port definiti in `core/data/**` (es. `ReportResultsCodec`, `DocumentWriter`, repository) ma non dalle loro impl.

Esempi consentiti:

- Un `UseCase` in `core/domain/usecase/**` che richiede un `core/data/repository/*Repository` nel costruttore.
- Un adapter in `data/repositoryimpl/**` che converte DTO Room/Retrofit in modelli dominio e implementa un port.

Esempi vietati:

- Un `UseCase` che crea un `JsonAdapter` Moshi o importa `com.app.miklink.data.repository.*`.
- Un port in `core/data/**` che espone `android.net.Uri` o classi Retrofit/Moshi.
- Una composable che importa direttamente un repository `data/**` o un DTO.
>>>>>>> aec31fe18138fb571fc1c1b9dd890bac55425d41
