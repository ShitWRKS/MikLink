(auto) Managed Virtual Device configurato: eseguire test con comando:

```
powershell -ExecutionPolicy Bypass ./gradlew.bat api34DebugAndroidTest
```

Se fallisce installazione su device fisico, usare il managed device.

---

## Stato corrente del progetto (audit 2025-12-09)

- Attenzione: la build completa attualmente fallisce durante la fase di processing KSP a causa di errori di compilazione in `app/src/main/java/com/app/miklink/data/pdf/PdfGeneratorIText.kt`. Prima di lanciare una pipeline CI completa, risolvere gli errori in quel file o applicare una compatibilità temporanea (wrapper `PdfGenerator`).

- Alcuni unit test risultano verdi in snapshot (vedi `test_results.log`) ma la pipeline di build/manuale può comunque fallire — verificare sempre `./gradlew clean assembleDebug` e analizzare i file `build_log_utf8.txt` e `compile_errors.txt` in caso di fallimento.

- File sensibili individuati: `key` nella root del repository. Pianificare una rimozione dalla VCS (git-filter-repo/BFG) e aggiornare `.gitignore` per impedire ricommitt. Questo richiede consenso del team.

Suggerimento rapido per debug locale:

```
# Compilazione completa (Windows PowerShell)
.
\gradlew.bat clean assembleDebug > build_log_utf8.txt 2>&1
# Controllare build_log_utf8.txt per errori KSP/compilazione

# Eseguire unit tests
.
\gradlew.bat testDebugUnitTest --no-daemon --console plain | tee test_results.log
```

