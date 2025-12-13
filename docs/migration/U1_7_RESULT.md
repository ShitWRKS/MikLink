# U1.7 Progressive Reveal Cards — Result

## Descrizione breve ✅
Implementata la regola "progressive reveal" per le card nella schermata `TestInProgressView`.
Durante l'esecuzione (isRunning == true): vengono mostrate tutte le sezioni con status != PENDING e al massimo la prima sezione PENDING (la prossima/attuale). Le card diventano espandibili solo quando lo step è finale (PASS/FAIL/SKIP). Le sezioni RUNNING/PENDING mostrano solo header (titolo + stato) e non sono espandibili né mostrano dettagli.

## File toccati 🔧
- `app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt` — aggiunta della logica `visibleSections`, `isFinalStatus`, estrazione di `TestSectionDetails` e uso di `expandable`.
- `app/src/main/java/com/app/miklink/ui/common/ResultCards.kt` — aggiunto parametro `expandable` a `TestSectionCard` per disabilitare l'espansione e il rendering dei dettagli quando non permesso.

## Verifica e conferma ✅
- Implementazione rispetta le regole richieste:
  - Durante RUNNING: appaiono progressivamente (conclusi + corrente/prossimo).
  - Card con status PENDING/RUNNING non sono espandibili e non mostrano dettagli.
  - Card con status PASS/FAIL/SKIP sono espandibili e mostrano i dettagli (stesso rendering della schermata finale).
- Non sono stati introdotti eventi di dominio o cambi al `RunTestUseCaseImpl`.

## Log baseline/finali
- Baseline KSP: `docs/migration/U1_7_ksp_baseline.txt`
- Baseline Assemble: `docs/migration/U1_7_assemble_baseline.txt`
- Baseline Tests: `docs/migration/U1_7_tests_baseline.txt`

- Final KSP: `docs/migration/U1_7_ksp_final.txt`
- Final Assemble: `docs/migration/U1_7_assemble_final.txt`
- Final Tests: `docs/migration/U1_7_tests_final.txt`

## Acceptance Criteria
- `:app:kspDebugKotlin` → PASS (vedi `U1_7_ksp_final.txt`) ✅
- `assembleDebug` → PASS (vedi `U1_7_assemble_final.txt`) ✅
- `testDebugUnitTest` → PASS (vedi `U1_7_tests_final.txt`) ✅
- `docs/migration/U1_7_RESULT.md` presente ✅

---
Se vuoi, posso:
- aggiungere test unitari per il comportamento visivo (es. test su `TestViewModel`/`RunTestUseCaseImpl` erano già presenti e passano), oppure
- scrivere test di integrazione/compose che verifichino che in `TestInProgressView` le card visibili e l'espandibilità rispettino la regola.

Dimmi come preferisci procedere (aggiungere test UI, aggiornare la documentazione del changelog, ecc.).
