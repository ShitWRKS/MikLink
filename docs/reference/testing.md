# Testing

Questa pagina descrive la strategia test e dove mettere nuove verifiche.

## Suite “bussola” (anti‑regressione)

Non negoziabili:

1) **Golden parsing tests** (fixture RouterOS + Moshi)  
   Path: `app/src/test/java/com/app/miklink/data/remote/mikrotik/golden/*`

2) **Quality tests**
   - `HardcodedStringsScanTest`: fallisce se compaiono stringhe hardcoded in UI
   - `StringsItalianCoverageTest`: copertura IT dove richiesto

3) **Contract/UseCase tests**
   - `RunTestUseCaseImplTest`
   - contract test su repository principali

## Come eseguire

```bash
./gradlew test
```

Per il catalogo nativo su un singolo device con dati locali da preservare, compilare
gli APK, aggiornarli con `adb install -r -t` e avviare direttamente
AndroidJUnitRunner:

```powershell
$serial = "<serial>"
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
adb -s $serial install -r -t .\app\build\outputs\apk\debug\app-debug.apk
adb -s $serial install -r -t .\app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
adb -s $serial shell am instrument -w -r `
  -e sessionId "native-<timestamp>" `
  com.app.miklink.test/androidx.test.runner.AndroidJUnitRunner
```

Le classi possono essere selezionate aggiungendo
`-e class <nome-completo>` a `am instrument`. Gli esiti
sono quattro: `PASS` (assertion eseguite), `FAIL` (violazione durante una sessione
eseguibile), `NOT_RUN` (prerequisito richiesto assente prima della valutazione) e
`SKIP` (passo opzionale/non applicabile). Nessun prerequisito mancante può essere
convertito in `PASS` o in un falso errore prodotto.

Gli artefatti accettabili includono manifest e risultato JSON conformi ai contratti,
screenshot con gerarchia semantica correlata, trace sanitizzato, logcat/process-exit
mirato e file generati dal caso. Devono essere relativi alla directory di sessione,
avere digest e identità build/device, e superare la scansione anti-segreti.

Assegnare un `sessionId` univoco per aggregare i risultati mantenuti. Ogni scenario
pubblica stato, reason code e percorso attraverso lo stream instrumentation; il
manifest e i risultati sono recuperabili senza wrapper proprietari:

```powershell
adb -s $serial shell am instrument -w -r `
  -e sessionId "native-<timestamp>" `
  -e class "com.app.miklink.e2e.catalog.ClientScenarioTest" `
  com.app.miklink.test/androidx.test.runner.AndroidJUnitRunner
adb -s $serial pull `
  "/storage/emulated/0/Android/data/com.app.miklink/files/agent-tests/native-<timestamp>" `
  "app/build/outputs/agent-tests/native-<timestamp>"
```

Un prerequisito assente viene scritto come `NOT_RUN` e poi adattato a una assumption
JUnit soltanto per consentire alle altre classi selezionate di continuare.

`connectedDebugAndroidTest` automatizza installazione e cleanup, ma va usato solo
quando lo stato locale del device è esplicitamente usa-e-getta. Su alcuni installer
OEM un rifiuto dell'APK di test può attivare la disinstallazione di cleanup e quindi
cancellare i dati dell'app. Con `disposableLocalState=false`, un errore di
`adb install -r -t` deve invece interrompere la sessione come `NOT_RUN`: non
disinstallare, non eseguire `pm clear` e non tentare un ripristino implicito.

## Live probe E2E (Windows)

Run the live probe instrumentation class from PowerShell without WSL or Git Bash:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\agent\run_live_probe_e2e.ps1
```

## Ispezione agentica nativa (solo debug)

La superficie agentica non Ã¨ un'opzione dell'app: Ã¨ compilata esclusivamente nel
source set `debug`. Non esistono flag runtime, extra di Intent, argomenti di
instrumentation, componenti esportati o impostazioni capaci di abilitarla in una
build `release`.

Selezionare sempre un solo device e verificare prima API e debuggabilitÃ :

```powershell
$serial = "<serial>"
adb devices -l
adb -s $serial shell getprop ro.build.version.sdk
adb -s $serial shell getprop ro.product.model
.\gradlew.bat :app:assembleDebug
adb -s $serial install -r -t .\app\build\outputs\apk\debug\app-debug.apk
adb -s $serial shell run-as com.app.miklink id
```

Proseguire solo con stato `device`, API 30 o superiore e `run-as` riuscito. Un
fallimento di `run-as` classifica la sessione `NOT_RUN`: non tentare di aggirarlo e
non eseguire la procedura su APK di produzione.

Lifecycle, osservazione e input devono essere limitati all'app selezionata:

```powershell
$sessionDir = "app/build/outputs/agent-tests/ad-hoc/<session-id>"
New-Item -ItemType Directory -Force -Path $sessionDir
adb -s $serial shell am force-stop com.app.miklink
adb -s $serial shell am start -W -n com.app.miklink/.MainActivity
adb -s $serial shell uiautomator dump /sdcard/miklink-window.xml
adb -s $serial pull /sdcard/miklink-window.xml "$sessionDir/ui-hierarchy.xml"
adb -s $serial exec-out screencap -p > "$sessionDir/screenshot.png"
adb -s $serial shell input tap <x> <y>
adb -s $serial shell input text '<non-secret-text>'
adb -s $serial shell input swipe <x1> <y1> <x2> <y2> <duration-ms>
adb -s $serial shell input keyevent KEYCODE_BACK
```

I tag stabili come `dashboard_screen`, `client_add_button` e
`test_execution_screen` compaiono come resource ID soltanto in debug. Dopo ogni
azione significativa ricatturare gerarchia e screenshot; scrivere l'esito terminale
in `scenario-result.json` e il manifest in `manifest.json`, quindi validarli contro
`specs/001-native-agent-testing/contracts/scenario-result.schema.json` e
`session-manifest.schema.json`.

Sono vietati: credenziali in riga di comando o file di evidenza, dump globali non
filtrati, accesso o modifica di dati di altre app, cancellazione dello storage
utente, modifica diretta del database e operazioni distruttive sulla sonda. Le sole
fixture modificabili sono quelle create dalla sessione; la configurazione della
sonda passa esclusivamente dalla normale UI dell'app.

### Checklist per la revisione UI/UX

Ogni osservazione fattuale deve essere formulata soltanto dopo aver acquisito
evidenza nella sessione corrente. Prima di accettare una revisione verificare che:

- ogni affermazione sulla UI rimandi a una gerarchia semantica o a uno screenshot
  corrente elencato nel manifest;
- un'azione che cambia stato abbia evidenze distinte `before` e `after`, correlate
  allo stesso `actionId` e acquisite rispettivamente prima e dopo l'input;
- la descrizione distingua ciò che è semanticamente verificato da ciò che è solo
  visibile nello screenshot;
- uno stato non raggiunto non venga descritto: il risultato deve elencarlo tra gli
  stati non osservati e indicare il passo o prerequisito che lo ha impedito;
- schermata bloccata, device perso, app non avviabile o build non debug producano
  `NOT_RUN` con un reason code esplicito, senza inferenze sul contenuto nascosto;
- file, digest, timestamp, device, build e correlazioni appartengano tutti alla
  stessa sessione, e la scansione anti-segreti sia passata.

Il confronto pixel-perfect non fa parte del contratto v1: screenshot e stato
semantico sono evidenze complementari, non baseline grafiche intercambiabili.

Per una verifica release esterna, installare l'APK release esatto e confermare che
`adb shell run-as com.app.miklink id` fallisca e che i tag agentici non siano esposti
come resource ID. Qualsiasi esito diverso blocca la distribuzione.

Le policy distruttive sono indipendenti e false per default. `disposableLocalState`
autorizza soltanto il reset/import locale del caso backup; `allowWifiDisruption`
richiede inoltre `hostControlRetained=true`, un device fisico designato, un limite
temporale e la verifica del ripristino in `finally`. Non esiste un flag generico
“unsafe”. Credenziali, fallback di sonda e amministrazione RouterOS fuori dall'app
restano vietati.

## Aggiungere un Golden test (ricetta)

1) Aggiungi la fixture in `app/src/test/resources/fixtures/<categoria>/<nome-file>` (se già presente, riusa il percorso esistente).
2) Caricala usando `FixtureLoader`.
3) Parsala con `TestMoshiProvider`.
4) Confronta:
   - campi obbligatori
   - edge cases (null, array vuoti)
   - mapping di tipi/enums

> Obiettivo: “se RouterOS cambia output, o se rompiamo il parsing, lo vediamo subito”.

## Linee guida

- Test deterministici (niente clock/random non controllati).
- Evita di testare dettagli UI se non necessari: la UI dovrebbe consumare modelli già “puliti”.
