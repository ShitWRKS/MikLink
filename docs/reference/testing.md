# Testing

## Piramide e regola di fedeltà

La copertura è classificata separatamente:

1. unit / golden / contract;
2. instrumented integration;
3. Functional UI Acceptance su device fisico;
4. Live-hardware acceptance con sonda configurata;
5. validazione esplorativa ad-hoc dell'agent.

I test `catalog/*ScenarioTest` che usano direttamente repository o use case restano
utili integration test. Non sono Functional UI Acceptance.

> Le fixture possono preparare uno scenario. Non possono sostituire tramite API
> interne la funzionalità che lo scenario dichiara di testare.

## Gate locali

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lint
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
```

Le suite bussola comprendono golden parsing RouterOS, quality scan, contract e use
case. Nuove fixture golden vanno in
`app/src/test/resources/fixtures/<categoria>/<nome-file>` e devono coprire campi
obbligatori, valori null/vuoti e mapping di tipi/enumerazioni.

## Preflight del device

Usare un solo seriale esplicito, API 30 o superiore:

```powershell
$serial = "<serial>"
adb devices -l
adb -s $serial shell getprop ro.build.version.sdk
adb -s $serial shell getprop ro.product.model
```

Il preflight esegue wake e controlla il keyguard. Se il device è bloccato pubblica
`DEVICE_UNLOCK_REQUIRED`: sbloccarlo manualmente con PIN/password/biometria e
lasciare collegato ADB. Lo stesso comando prosegue appena il keyguard è rimosso. Se
il limite scade, il risultato è `NOT_RUN / DEVICE_LOCKED`; il lock iniziale non è un
product FAIL e l'automazione non conosce né inserisce credenziali.

Zero device, target multipli non selezionati, `offline` e `unauthorized` sono
prerequisiti non disponibili. Non selezionare un device arbitrariamente.

## Installazione preservando i dati

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
adb -s $serial install -r -t .\app\build\outputs\apk\debug\app-debug.apk
adb -s $serial install -r -t .\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
```

Con `disposableLocalState=false`, un errore di installazione termina `NOT_RUN`: non
disinstallare, non eseguire `pm clear` e non ripristinare implicitamente dati.
`connectedDebugAndroidTest` è ammesso solo quando lo stato locale è esplicitamente
usa-e-getta, perché il cleanup di alcuni installer OEM può disinstallare i package.

## Functional UI Acceptance

Scenario mirato:

```powershell
adb -s $serial shell am instrument -w -r `
  -e sessionId "functional-client-<timestamp>" `
  -e class "com.app.miklink.e2e.functional.ClientCrudUiTest" `
  com.app.miklink.test/androidx.test.runner.AndroidJUnitRunner
```

Suite completa:

```powershell
adb -s $serial shell am instrument -w -r `
  -e sessionId "functional-full-<timestamp>" `
  -e class "com.app.miklink.e2e.functional.FunctionalAcceptanceSuite" `
  com.app.miklink.test/androidx.test.runner.AndroidJUnitRunner
```

Le classi indipendenti coprono avvio/navigazione, CRUD cliente, CRUD profilo,
impostazioni, impostazioni report, storico/dettaglio ed export PDF. I record hanno
nomi E2E univoci. Poiché l'attuale UI non espone Delete per Clienti o Profili, la
rimozione finale è cleanup ID-scoped e non viene dichiarata copertura UI di Delete.

Per rieseguire una correzione, invocare prima la singola classe e poi la suite. Ogni
esecuzione deve usare un nuovo `sessionId` per legare risultato, build e device.

## Live probe

Configurare la sonda dalla normale UI e invocare direttamente AndroidJUnitRunner.
Indirizzi e segreti non sono argomenti del comando:

```powershell
adb -s $serial shell am instrument -w -r `
  -e sessionId "live-<timestamp>" `
  -e class "com.app.miklink.e2e.LiveProbeE2ETest" `
  com.app.miklink.test/androidx.test.runner.AndroidJUnitRunner
```

Il flusso seleziona client/profilo, avvia dalla UI, osserva Running e Completed,
verifica le sezioni prodotte, salva, apre lo storico e il dettaglio, quindi correla
request, response/error, parsing, normalization, decisione e risultato visibile.
Speed è `NOT_RUN` senza server configurato. Capability opzionali non applicabili
sono `SKIP`; non esistono PASS fittizi.

La parity del workflow nativo è stata accettata e i runner host legacy sono stati
rimossi. Gradle, ADB e AndroidJUnitRunner costituiscono l'unico workflow supportato.

## Risultati e artefatti

Gli esiti sono `PASS`, `FAIL`, `NOT_RUN`, `SKIP`. Gli artefatti ammessi sono:

- manifest e `scenario-result.json`;
- `before.png`, `after.png`, `failure.png` quando applicabili;
- `ui-hierarchy.xml`;
- trace NDJSON sanitizzato e targeted logcat;
- `report.pdf` o altri file prodotti dallo scenario.

Non generare screen recording o video. Acquisire screenshot soltanto prima/dopo
azioni critiche, sul risultato finale e sempre su failure; trace e hierarchy sono
preferibili a sequenze di immagini.

Recupero diretto senza wrapper:

```powershell
adb -s $serial pull `
  "/storage/emulated/0/Android/data/com.app.miklink/files/agent-tests/<session-id>" `
  "app/build/outputs/agent-tests/<session-id>"
```

I file devono essere elencati dal manifest, avere digest e identità build/device,
rispettare i contratti in `specs/001-native-agent-testing/contracts/` e superare la
scansione anti-segreti. I backup non vengono conservati come evidenza.

## Esplorazione agentica debug-only

Verificare prima che `run-as com.app.miklink id` riesca. La release non espone la
superficie semantica agentica e non ha flag, Intent, instrumentation argument,
componente o impostazione capace di abilitarla.

```powershell
adb -s $serial shell am force-stop com.app.miklink
adb -s $serial shell am start -W -n com.app.miklink/.MainActivity
adb -s $serial shell uiautomator dump /sdcard/miklink-window.xml
adb -s $serial pull /sdcard/miklink-window.xml <session-dir>/ui-hierarchy.xml
adb -s $serial exec-out screencap -p > <session-dir>/screenshot.png
```

Individuare il controllo tramite resource ID, testo o content description nel dump,
ricavare dinamicamente i `bounds` correnti e solo allora eseguire il tap al loro
centro. Coordinate costanti o registrate in precedenza sono vietate. Dopo
l'interazione acquisire il nuovo stato semantico.

Sono vietati credenziali nei comandi/evidenze, dump globali non filtrati, modifica di
dati di altre app, cancellazione dello storage senza opt-in, modifica diretta del DB
per simulare lo scenario, backchannel RouterOS, pixel diff e descrizioni di UI non
osservata. `disposableLocalState` e `allowWifiDisruption` sono autorizzazioni
indipendenti; la seconda richiede anche `hostControlRetained=true` e ripristino
verificato in `finally`.
