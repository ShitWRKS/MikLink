# Build & Tooling

## Requisiti

- Android Gradle Plugin (AGP): **9.3.1**
- Gradle wrapper: **9.6.1**
- JDK: **17**
- Kotlin/KGP: **2.4.10**
- KSP: **2.3.10**
- Compose compiler plugin: **2.4.10**
- Compile SDK: **37**
- Min SDK: **30**
- Target SDK: **36**

Version catalog: `gradle/libs.versions.toml`.

## Dipendenze principali (baseline corrente)

- Compose BOM: **2026.06.01**
- Hilt: **2.60.1** (Dagger) + AndroidX Hilt **1.4.0**
- Room: **2.8.4** (plugin Room centralizzato nel catalog)
- Retrofit: **3.0.0** + OkHttp **5.4.0**
- Moshi: **1.15.2** + Moshi codegen **1.15.2** (KSP)
- Coroutines: **1.11.0**
- Core KTX: **1.19.0**
- Lifecycle: **2.11.0**
- Activity Compose: **1.13.0**
- Navigation Compose: **2.9.8**
- DataStore: **1.2.1**
- Tracing: **1.3.0**
- Coil: **3.5.0** (`coil-compose` + `coil-gif`)
- AndroidX Test JUnit: **1.3.0**
- Espresso: **3.7.0**
- MockK: **1.14.11**
- Robolectric: **4.16.1**
- iText Core Android: **9.7.1** (`com.itextpdf.android:itext-core-android`)

Vietato: versioni pre-release, version range/dinamici e repository snapshot.
`com.google.android.material:material` rimosso (non usato dal codice).

## Annotation processing (KSP/KAPT)

- Plugin: `com.google.devtools.ksp` applicato al modulo `app`.
- Processor KSP:
  - `room-compiler` (schema esportato in `app/schemas/**`)
  - `hilt-compiler` (KSP attivo in questo progetto; monitorare stabilità)
- Processor KAPT: _nessuno_ (kapt non applicato al momento).

### Rationale
- Room: KSP è raccomandato da Android (miglior build perf / supporto Kotlin 2/KSP2).
- Hilt/Dagger: KSP support è ancora segnalato come alpha nei docs Dagger, ma è abilitato qui; se emergono problemi, valutare fallback a kapt (applicando `kotlin("kapt")` e sostituendo `ksp(libs.hilt.compiler)` con `kapt(...)`).

### Migrazione/futuro
- Continuare a preferire KSP dove disponibile.
- Se si introduce un processor senza KSP stabile, aggiungere `kapt` solo per quel caso e documentarlo qui.
- Monitorare le note di rilascio:
  - “Migrate from kapt to KSP”: https://developer.android.com/build/migrate-to-ksp
  - Room release notes (KSP/KSP2): https://developer.android.com/jetpack/androidx/releases/room
  - Dagger KSP status: https://dagger.dev/dev-guide/ksp.html

## Comandi utili

```bash
# unit test
./gradlew test

# quality gate (inclusi scan stringhe)
./gradlew check

# build debug
./gradlew assembleDebug

# build release (minified, R8 enabled)
./gradlew assembleRelease

# instrumentation (se presenti test strumentati)
./gradlew connectedAndroidTest
```

## Release Build

La release build è configurata in `app/build.gradle.kts`:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### ProGuard/R8

Le regole ProGuard sono definite in `app/proguard-rules.pro`:
- **Moshi DTOs**: tutti i DTO usano `@JsonClass(generateAdapter = true)` per compatibilità R8
- **Retrofit/OkHttp**: regole standard per reflection e platform detection
- **iText Core Android**: nessuna keep rule globale; un `-dontwarn` puntuale copre la factory Bouncy Castle non-Android opzionale non usata dal percorso PDF MikLink

### Logging

I log statements sono condizionati a `BuildConfig.DEBUG`:
- In release, i log non vengono eseguiti
- Pattern: `if (BuildConfig.DEBUG) Log.d(...)`

### Signing

Per pubblicare su Play Store, configurare `signingConfigs` in `app/build.gradle.kts`:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("path/to/keystore.jks")
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ...
    }
}
```

## Note

- **Versione DB**: definita in `@Database(version = X)` in `MikLinkDatabase.kt`
- **Schema export**: esportato in `app/schemas/com.app.miklink.data.local.room.MikLinkDatabase/<version>.json`
- **Migrazioni**: Per schema changes futuri, implementare `Migration` esplicite invece di destructive migration
