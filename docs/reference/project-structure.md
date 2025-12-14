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

- Un file in `core/domain/**` ha import `android.*`? → **errore**
- Un file in `ui/**` importa `room`/`dao`/`entity`? → **errore**
- Un file in `data/**` importa `ui/**`? → **errore**
- Esiste `probeId` in qualsiasi file Kotlin? → **errore**
