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
