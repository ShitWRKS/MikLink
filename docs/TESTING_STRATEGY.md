# MikLink — Testing Strategy

This document summarizes the testing strategy for MikLink and the way golden fixtures
are used for deterministic parsing/contract tests.

Pyramid:
- Domain unit tests (core/domain) — logic contracts
- Data integration / parsing (core/data) — golden fixtures
- ViewModel minimal tests — minimal mapping from domain to view state
- UI instrumentation: minimal or none in this EPIC

Golden fixtures RouterOS:
- Fixtures live in `app/src/test/resources/fixtures/routeros/7.20.5/`
- They are copies of outputs collected from real routers using the commands below
- Tests assert parsing correctness; when parsing fails, update DTO/mappers, do not change fixtures

Commands used to collect fixtures:
- `curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/system/resource?.proplist=.id,time,topics,message"`
- `curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/ip/neighbor"`
- `curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/ethernet/monitor?interface=ether1"`
- `curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/ethernet/cable-test?interface=ether1"`
- `curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/bridge/host"`
- `curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/bridge/port"`

Fixtures sensitive notes:
- `bridge_port.json` contains debug-info (multiline) and must be left unmodified
- `log_get_proplist.json` is a long array and used for log filtering tests

Legacy tests
- Legacy test suites were removed in EPIC T1 and replaced with golden fixtures (core/data) and contract tests (core/domain)

Test running
- Local tests are executed via the existing gradle task `./gradlew testDebugUnitTest` (or `./gradlew test` depending on modules). No CI changes in this EPIC.

TODOs:
- Add small fixtures for more RouterOS outputs as integrations increase
- Align `TestMoshiProvider` to production once DI modules are stabilized

Hardcoded Strings Guard (EPIC U2.2) — Italiano
------------------------------------------------

Questa suite include un test JVM che rileva stringhe hardcoded nelle UI Compose.

Come lanciare:

```powershell
./gradlew.bat testDebugUnitTest --tests "com.app.miklink.quality.HardcodedStringsScanTest"
./gradlew.bat testDebugUnitTest --tests "com.app.miklink.quality.StringsItalianCoverageTest"
```

Comportamento:
- Il test `HardcodedStringsScanTest` scansiona `app/src/main/java/com/app/miklink/ui/` per le occorrenze di:
	- `Text("...")`
	- `Text(text = "...")`
	- `contentDescription = "..."`
- Se trova violazioni il test fallisce con messaggi del tipo:

```
HARD_CODED_UI_TEXT: <file>:<line> -> <snippet>

FIX (standard)
crea una key in res/values/strings.xml
crea la traduzione in res/values-it/strings.xml
sostituisci con stringResource(R.string.<key>) (o context.getString(...) se non sei in composable)
```

Regole di ignoramento e allowlist:
- Aggiungi `// i18n-ignore` sulla stessa riga quando la stringa hardcoded è intenzionale e giustificata (es. test fixture, costante tecnica). Il commento impedisce il fail del test.
- Sono ignorate stringhe vuote `""` e stringhe costituite solo da simboli tecnici (es. `"---"`, `":"`, `%`).

Note:
- Questo non è un lint personalizzato: è un unit test JVM che fallisce la build quando vengono introdotte stringhe hardcoded in UI.
- Se il test segnala falsi positivi, aggiorna l'allowlist in `HardcodedStringsScanTest.kt` e aggiungi il caso di test pertinente.

