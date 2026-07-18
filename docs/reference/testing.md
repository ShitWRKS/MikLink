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

## Live probe E2E (Windows)

Run the live probe instrumentation class from PowerShell without WSL or Git Bash:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\agent\run_live_probe_e2e.ps1
```

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
