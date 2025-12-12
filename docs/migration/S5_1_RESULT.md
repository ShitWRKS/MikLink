# S5.1 – Risultato

## File toccati
- `app/src/main/java/com/app/miklink/ui/test/TestViewModel.kt` (rimossi blocchi legacy commentati)
- `app/src/main/java/com/app/miklink/core/data/repository/test/NetworkConfigRepository.kt` (aggiunto KDoc + @Deprecated)
- `app/src/main/java/com/app/miklink/data/repositoryimpl/NetworkConfigRepositoryImpl.kt` (aggiunto KDoc + @Deprecated)
- `app/src/main/java/com/app/miklink/core/data/repository/test/PingTargetResolver.kt` (nuovo)
- `app/src/main/java/com/app/miklink/data/repositoryimpl/PingTargetResolverImpl.kt` (nuovo)
- `app/src/main/java/com/app/miklink/data/teststeps/PingStepImpl.kt` (rimosso AppRepository, usa PingTargetResolver)
- `app/src/main/java/com/app/miklink/core/domain/test/model/PingTargetOutcome.kt` (nuovo)
- `app/src/main/java/com/app/miklink/core/domain/test/model/TestSectionResult.kt` (aggiunto campo `title`)
- `app/src/main/java/com/app/miklink/core/domain/test/model/TestSkipReason.kt` (nuovo: reason codes)
- `app/src/main/java/com/app/miklink/core/domain/usecase/test/RunTestUseCaseImpl.kt` (rawResultsJson deterministico, mapping sections)
- `app/src/main/java/com/app/miklink/di/RepositoryModule.kt` (binding PingTargetResolver)
- `app/src/main/java/com/app/miklink/ui/test/TestSkipReasonMapper.kt` (nuovo: mapping reason codes → stringhe localizzate)
- `app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt` (usa TestSkipReasonMapper per reason)
- `app/src/main/java/com/app/miklink/data/teststeps/SpeedTestStepImpl.kt` (usa TestSkipReason)
- `app/src/main/res/values/strings.xml` (aggiunte stringhe skip_reason_*)
- `app/src/main/res/values-it/strings.xml` (aggiunte stringhe skip_reason_*)

## Cosa è stato rimosso
- Eliminati i 600+ righi di orchestrazione legacy commentati da `TestViewModel.kt`.

## PingStep e AppRepository
- `PingStepImpl` ora usa `PingTargetResolver` dedicato; non dipende più da `AppRepository`.

## Contratto PingTargetResolver
- Il parametro `probe: ProbeConfig` è **necessario** perché serve per:
  - costruire il service MikroTik tramite `buildServiceFor(probe)`
  - chiamare `api.getDhcpClientStatus(interfaceName)` per risolvere il gateway DHCP
- Documentato in KDoc del contratto che è un bridge temporaneo verso EPIC S6.

## Raw results / mapping
- `RunTestUseCaseImpl` serializza sempre `rawResultsJson` in formato deterministico (timestamp, plan, steps).
- Ogni Step genera un `TestSectionResult` con `title`, `status`, `details`.
- Struttura JSON v1: `{timestamp, plan: {clientId, probeId, profileId, socketId}, steps: [{name, status, data?, error?}]}`

## Reason codes per Skip
- Sostituite stringhe hardcoded (`"Nessun target configurato"`, `"Server speed test non configurato"`) con reason codes stabili (`TestSkipReason.PING_NO_TARGETS`, `TestSkipReason.SPEED_NO_SERVER`).
- Mapping UI: `TestSkipReasonMapper` converte reason codes in stringhe localizzate (IT/EN).
- I reason codes vengono mostrati all'utente nella UI tramite `details["reason"]` → mapping automatico quando `d.label == "reason"`.

## Comandi eseguiti
- ✅ `./gradlew :app:kspDebugKotlin` → PASS (output in `docs/migration/S5_1_ksp.txt`)
- ✅ `./gradlew assembleDebug` → PASS (output in `docs/migration/S5_1_assemble.txt`)
- ✅ `./gradlew testDebugUnitTest` → PASS (output in `docs/migration/S5_1_tests.txt`)

## Breaking changes / contract update
- Nessun breaking change per l'utente finale.
- `TestSectionResult` ora include `title` (campo aggiunto, non breaking per serializzazione JSON esistente).
- Reason codes: i vecchi report con stringhe hardcoded continuano a funzionare (fallback nel mapper).

