# Production readiness

- `./gradlew check` è il quality gate canonico: include `:app:check`, lint, test e controlli repository.
- Il Gradle Wrapper 9.5.0 è versionato; non è rigenerato dalla CI.
- Room usa migrazioni non distruttive 1→2 e 2→3, testate rispetto agli schema esportati.
- Un report resta nella schermata finché il salvataggio non riesce; in caso di errore si può riprovare.
- Se Layer 1 fallisce, le sezioni dipendenti ancora non concluse sono marcate `SKIP` con motivo `LAYER1_FAILED`.
- Le stringhe UI sono risorse localizzate; i conteggi visibili devono usare plurali.
- I push su `develop` pubblicano una prerelease; i push su `master` pubblicano una release stabile. Le PR eseguono `./gradlew check`.
- Il codice originale di MikLink è MIT; i componenti di terze parti restano soggetti alle rispettive licenze.

## Live probe E2E

`LiveProbeE2ETest` richiede una sonda RouterOS fisica e configurazione locale. Non fa parte della build standard né delle GitHub Actions e non è richiesto per approvare una PR. Eseguirlo manualmente durante lo sviluppo delle integrazioni con la sonda:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.app.miklink.e2e.LiveProbeE2ETest
```

## Gate agentico nativo

La capacità agentica è un artefatto di sviluppo: entry point E2E, resource ID
semantici e trace avanzato esistono soltanto nella variante `debug`. La variante
`release` usa una policy semantica disabilitata e un sink di trace senza scritture;
non espone flag runtime, extra di Intent, argomenti di instrumentation, componenti
esportati o impostazioni per riattivare il controllo.

Prima di una release eseguire:

```bash
./gradlew testDebugUnitTest lint assembleRelease
```

Sul device designato installare l'APK release firmato esatto e verificare dall'esterno:

- `run-as com.app.miklink` deve fallire;
- extra/azioni Intent come `agentMode`, `e2eMode` e `testControl` non devono cambiare
  il comportamento;
- argomenti AndroidJUnitRunner non devono trovare un runner nel pacchetto release;
- il manifest non deve contenere componenti agent/E2E/test-control esportati;
- la gerarchia non deve esporre i test tag come resource ID e non devono comparire
  file `debug_trace_*.ndjson` o log `MIKLINK_E2E_TRACE`;
- launch e navigazione black-box rappresentativa devono restare funzionanti.

I test con sonda richiedono una configurazione esplicita già presente nell'app;
assenza/autenticazione/capacità/speed server producono `NOT_RUN` o `SKIP` mirati.
Backup con sostituzione dati e interruzione Wi‑Fi restano esclusi senza i rispettivi
opt-in; la Wi‑Fi richiede anche controllo host USB/ADB trattenuto e ripristino
verificato. La parity nativa è accettata: il workflow supportato usa direttamente
Gradle, ADB e AndroidJUnitRunner, senza runner host di compatibilità.
