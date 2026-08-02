plugins {
    alias(libs.plugins.android.application)
    // AGP 9.x has built-in Kotlin support: the org.jetbrains.kotlin.android plugin is no longer applied.
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp) // Annotation processing defaults to KSP; enable kapt only if a processor lacks KSP support.
    alias(libs.plugins.room)
}

val ciVersionCode = providers.environmentVariable("MIKLINK_VERSION_CODE")
    .orNull
    ?.toIntOrNull()
val ciVersionName = providers.environmentVariable("MIKLINK_VERSION_NAME").orNull
val releaseKeystorePath = providers.environmentVariable("MIKLINK_KEYSTORE_PATH").orNull
val releaseStorePassword = providers.environmentVariable("MIKLINK_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("MIKLINK_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("MIKLINK_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }

android {
    namespace = "com.app.miklink"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.app.miklink"
        minSdk = 30
        targetSdk = 36
        versionCode = ciVersionCode ?: 1
        versionName = ciVersionName ?: "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseKeystorePath))
                storeType = "PKCS12"
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    androidResources {
        localeFilters += listOf("en", "it")
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
        compilerOptions {
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

room {
    // Output Room schemas for inspection / migration tests
    schemaDirectory("$projectDir/schemas")
}

// KGP 2.3.21 emits Kotlin 2.4 metadata, but Hilt 2.59.2's javac aggregation step
// (hiltJavaCompile) bundles kotlin-metadata-jvm 2.2.20 which only reads up to 2.3.0.
// Force a metadata reader aligned with KGP so the Hilt javac step can parse Kotlin 2.4 metadata.
configurations.configureEach {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.21")
    }
}

dependencies {
    implementation(libs.coil.gif)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Hilt (running on KSP; if reverting, apply kotlin("kapt") and swap this to kapt)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Activity Compose
    implementation(libs.androidx.activity.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.kotlin.codegen)
    implementation(libs.okhttp)

    // DataStore
    implementation(libs.datastore.preferences)

    // PDF
    implementation(libs.itext7.core)

    // Images
    implementation(libs.coil.compose)

    // Tracing
    implementation(libs.androidx.tracing)

    // Tests
    testImplementation(libs.junit)
    testImplementation(enforcedPlatform(libs.kotlinx.coroutines.bom))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.test.manifest)
}