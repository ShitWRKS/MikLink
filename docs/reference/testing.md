# Testing

Questa pagina definisce la policy test durante il refactor.

## Suite “bussola” (anti-regressione)

Sono considerati **non negoziabili**:

1) **Golden parsing tests** (fixture RouterOS + Moshi)
2) **Quality tests**
   - scan hardcoded strings
   - coverage italiano delle stringhe

Altri test (contract placeholder, Compose UI test, migration) possono essere presenti,
ma non devono guidare scelte architetturali se in conflitto con ADR/architettura.

## Quando eseguire i test

Durante una unità di lavoro (epic/PR) è accettabile che:
- compilazione o test falliscano temporaneamente

Alla fine della unità di lavoro è obbligatorio che:
- `./gradlew test` sia **verde**
- se si toccano UI/Room: `./gradlew connectedAndroidTest` quando possibile

Se un test fallisce perché è cambiata l'intenzione:
- non “aggiustare” alla cieca
- registrare evidenza in `docs/DISCREPANCIES.md`
- decidere tramite aggiornamento scope o ADR

## Dove sono i test

- Unit test (JVM): `app/src/test/...`
- Instrumentation tests: `app/src/androidTest/...`

## Comandi

```bash
# unit test
./gradlew test

# instrumentation
./gradlew connectedAndroidTest
```

## Linee guida

- Golden test deterministici:
  - fixture JSON versionate
  - parsing con Moshi (provider test dedicato)
- Quality test:
  - fallire se compaiono stringhe hardcoded in UI
  - fallire se mancano traduzioni IT dove richiesto
