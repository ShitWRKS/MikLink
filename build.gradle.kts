// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt.gradle) apply false
    alias(libs.plugins.ksp) apply false
}

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

fun countFiles(glob: String): Int {
    val matcher = java.nio.file.FileSystems.getDefault().getPathMatcher("glob:$glob")
    return Files.walk(project.projectDir.toPath())
        .filter { p -> matcher.matches(project.projectDir.toPath().relativize(p)) }
        .count().toInt()
}

fun replaceFirstRegex(content: String, pattern: Regex, replacement: String): String {
    val match = pattern.find(content) ?: return content
    return content.replaceRange(match.range, replacement)
}

tasks.register("docsUpdateProjectState") {
    group = "documentation"
    description = "Aggiorna data e statistiche in PROJECT_STATE_DOCUMENTATION.md"
    doLast {
        val docPath = project.rootDir.toPath().resolve("PROJECT_STATE_DOCUMENTATION.md")
        var text = Files.readString(docPath)

        // Update date at the first occurrence
        val today = LocalDate.now().toString()
        text = replaceFirstRegex(
            text,
            Regex("""\*\*Stato del Progetto al:\s*\d{4}-\d{2}-\d{2}\*\*"""),
            "**Stato del Progetto al: $today**"
        )

        // Compute stats
        val kotlinTotal = countFiles("**/app/src/main/java/**/*.kt")
        val screens = countFiles("**/app/src/main/java/**/ui/**/*Screen.kt")
        val viewModels = countFiles("**/app/src/main/java/**/ui/**/*ViewModel.kt")
        val entities = countFiles("**/app/src/main/java/**/data/db/model/*.kt")
        val daos = countFiles("**/app/src/main/java/**/data/db/dao/*.kt")
        val repositories = countFiles("**/app/src/main/java/**/data/repository/*.kt")

        // Replace stats numbers in the STATISTICHE PROGETTO section (first occurrence per label)
        text = text.replace(Regex("(?m)(- \\*\\*Total Kotlin Files\\*\\*: )\\~?\\d+"), "$1$kotlinTotal")
        text = text.replace(Regex("(?m)(- \\*\\*Screens\\*\\*: )\\~?\\d+"), "$1$screens")
        text = text.replace(Regex("(?m)(- \\*\\*ViewModels\\*\\*: )\\~?\\d+"), "$1$viewModels")
        text = text.replace(Regex("(?m)(- \\*\\*Database Entities\\*\\*: )\\~?\\d+"), "$1$entities")
        text = text.replace(Regex("(?m)(- \\*\\*DAOs\\*\\*: )\\~?\\d+"), "$1$daos")
        text = text.replace(Regex("(?m)(- \\*\\*Repositories\\*\\*: )\\~?\\d+"), "$1$repositories")

        Files.writeString(docPath, text, StandardOpenOption.TRUNCATE_EXISTING)
        println("PROJECT_STATE_DOCUMENTATION.md aggiornato: data=$today, kt=$kotlinTotal, screens=$screens, vms=$viewModels, entities=$entities, daos=$daos, repos=$repositories")
    }
}

// Inserire questa sezione vicino alla configurazione globale (root / top-level)
// Impone Java 21 per Kotlin compilation / toolchain dei subprojects
subprojects {
    // Se il subproject usa Kotlin, applica il jvmToolchain tramite l'estensione KotlinJvmProjectExtension
    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper> {
        // Try Kotlin JVM extension (non-Android modules)
        extensions.findByType(KotlinJvmProjectExtension::class.java)?.let { kext ->
            kext.jvmToolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }
        // For Android modules the Kotlin Android extension type is not available at root script
        // evaluation classpath; we rely on configuring Kotlin compile tasks below as a fallback.
    }

    // Applica Java toolchain a tutti i subprojects (compatibilità compilazione Java) usando JavaPluginExtension
    plugins.withId("java") {
        extensions.configure(JavaPluginExtension::class.java) {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }
    }

    // Assicura che il KotlinCompile generi bytecode targeting JVM 21
    // Preferiamo impostare il toolchain sopra; mantenere un fallback compatibile per kotlinOptions
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        // kotlinOptions è deprecato ma ancora compatibile su molte versioni del plugin.
        // Lasciamo un fallback qui per compatibilità evitando errori di receiver.
        try {
            @Suppress("DEPRECATION")
            this.kotlinOptions.jvmTarget = "21"
        } catch (e: Exception) {
            // best-effort, se la proprietà non è disponibile ignoriamo: il jvmToolchain già imposta il target
        }
    }
}

// NOTE: Avoid importing KotlinAndroidProjectExtension here because it's not available
// on the buildscript classpath for the root project evaluation.

// Nota: la configurazione subprojects per jvmToolchain è già presente e imposta Java 21.
// Nessuna modifica aggiuntiva funzionale richiesta qui.
