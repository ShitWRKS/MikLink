plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt.gradle)
    id("com.google.devtools.ksp")
    id("jacoco")
}

import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import javax.inject.Inject

// Imposta toolchain JVM per questo modulo (Java/Kotlin)
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

android {
    namespace = "com.app.miklink"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.app.miklink"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Room schema export per i test di migrazione
        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            enableUnitTestCoverage = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.all {
            it.testLogging {
                events("passed", "skipped", "failed")
            }
        }
        unitTests {
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        // Include Room schema JSON files in androidTest assets
        getByName("androidTest") {
            assets.srcDirs("$projectDir/schemas")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // Core & UI
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.room.ktx)
    
    // Networking
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.moshi)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.moshi.core)
    implementation(libs.moshi.kotlin)

    // Unit & Instrumentation test dependencies
    // Use version catalog entries for test dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.room.testing)
    // Tracing required by newer androidx.test/platform for proper Espresso tracing
    androidTestImplementation(libs.androidx.tracing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    // Compose UI test dipendenze dirette
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Forza una versione compatibile di androidx.tracing per evitare NoSuchMethodError durante instrumented tests
configurations.all {
    resolutionStrategy {
        // force tracing 1.1.0 which è compatibile con androidx.test/espresso
        force("androidx.tracing:tracing:1.1.0")
    }
}

// Disabilita KSP incremental come mitigazione per problemi di mappatura file su JVM recenti
// (KSP supporta il passaggio di argomenti tramite l'estensione 'ksp')
ksp {
    arg("ksp.incremental", "false")
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Garantire che tutte le compilazioni Kotlin usino jvmTarget 21
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// Jacoco Test Coverage Configuration
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.getByName("testDebugUnitTest"))
    group = "Reporting"
    description = "Generates Jacoco code coverage reports for the debug build."

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    val fileFilter = listOf(
        // Android generated classes
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",

        // Hilt generated classes
        "**/*_HiltModules*.*",
        "**/Hilt_*.class",
        "**/*_Factory.class",
        "**/*_Provide*Factory.class",
        "**/*_MembersInjector.class",
        "**/Dagger*Component.class",
        "**/Dagger*Component\$Builder.class",
        "**/*Subcomponent\$Builder.class",

        // UI, DI, and models
        "**/ui/**",
        "**/di/**",
        "**/data/db/model/**"
    )

    val buildDir = layout.buildDirectory.get().asFile
    val kotlinClasses = fileTree("$buildDir/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    classDirectories.setFrom(files(kotlinClasses))
    sourceDirectories.setFrom(files("$projectDir/src/main/java"))
    executionData.setFrom(fileTree(buildDir).include("outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"))
}

tasks.withType<Test> {
    finalizedBy("jacocoTestReport")
}

// Disables ProfileInstaller for instrumentation tests, which can cause hangs.
configurations.named("androidTestImplementation") {
    exclude(group = "androidx.profileinstaller", module = "profileinstaller")
}