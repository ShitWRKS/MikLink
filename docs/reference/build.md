# Build & Tooling

## Requisiti

- Android Gradle Plugin (AGP): **8.13.2**
- Kotlin: **2.2.21**
- Compile SDK: **36**
- Min SDK: **30**
- Target SDK: **36**

Version catalog: `gradle/libs.versions.toml`.

## Dipendenze principali (snapshot)

- Compose BOM: **2025.12.00**
- Hilt: **2.56.2**
- Room: **2.8.4**
- Retrofit: **2.11.0** + OkHttp **4.12.0**
- Moshi: **1.15.2**
- Coroutines: **1.10.2**
- iText: **7.2.6** (PDF)

## Comandi utili

```bash
# unit test
./gradlew test

# quality gate (inclusi scan stringhe)
./gradlew test

# build debug
./gradlew assembleDebug

# instrumentation (se presenti test strumentati)
./gradlew connectedAndroidTest
```

## Note

- Lo schema Room viene esportato in `app/schemas/**` (versione attuale: v1).
