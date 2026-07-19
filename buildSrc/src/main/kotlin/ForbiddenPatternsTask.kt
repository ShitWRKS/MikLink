import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ProjectLayout
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

private data class ForbiddenPattern(
    val regex: Regex,
    val message: String,
    val appliesOnlyIfPathContains: String? = null,
    val skipIfPathContains: String? = null
)

abstract class ForbiddenPatternsTask @Inject constructor(
    @get:Internal
    val layout: ProjectLayout
) : DefaultTask() {

    @get:Input
    abstract val roots: ListProperty<String>

    init {
        description = "Fails the build if deprecated/forbidden patterns are present in source or docs."
        group = "verification"
        roots.convention(listOf("app/src", "docs"))
    }

    @TaskAction
    fun check() {
        val patterns = listOf(
            ForbiddenPattern(
                regex = Regex("""fallbackToDestructiveMigration\(\s*\)"""),
                message = "Use fallbackToDestructiveMigration(dropAllTables = true)"
            ),
            ForbiddenPattern(
                regex = Regex("""\bTabRow\("""),
                message = "Use PrimaryTabRow or SecondaryTabRow"
            ),
            ForbiddenPattern(
                regex = Regex("""\bScrollableTabRow\("""),
                message = "Use PrimaryTabRow or SecondaryTabRow"
            ),
            ForbiddenPattern(
                regex = Regex("""centerAlignedTopAppBarColors\("""),
                message = "Replace with TopAppBarDefaults.topAppBarColors"
            ),
            ForbiddenPattern(
                regex = Regex("""@ApplicationContext\s+private\s+val"""),
                message = "Add @param: to the qualifier on constructor properties"
            ),
            ForbiddenPattern(
                regex = Regex("""@(?!(field:|param:))Json\([^)]*\)\s+(val|var)"""),
                message = "Apply explicit use-site target: @param:Json or @field:Json on Moshi-mapped constructor properties"
            ),
            // Fase 2/9: repository MikroTik non devono costruire Retrofit o usare la factory direttamente
            ForbiddenPattern(
                regex = Regex("""Retrofit\.Builder"""),
                message = "core/data and data/repository must not build Retrofit; use MikroTikCallExecutor"
            ),
            ForbiddenPattern(
                regex = Regex("""MikroTikServiceFactory"""),
                message = "MikroTikServiceFactory must not be used in repositories; use MikroTikCallExecutor"
            ),
            // DTO remoti non devono uscire da data/remote
            // (i file dentro data/remote possono usare i DTO: decoder/mapper vivono li')
            ForbiddenPattern(
                regex = Regex("""import com\.app\.miklink\.data\.remote\.mikrotik\.dto\."""),
                message = "Remote DTOs must not leave data/remote; map to domain models",
                skipIfPathContains = "data/remote"
            ),
            // Nessun fallback a {} nel flusso report
            ForbiddenPattern(
                regex = Regex("""getOrElse\s*\(\s*\{\s*"\{\}"\s*\}\s*\)"""),
                message = "Do not fall back to \"{}\" in the report serialization flow (ADR-0013)"
            ),
            // core/domain non deve importare Android/AndroidX/UI/data
            ForbiddenPattern(
                regex = Regex("""^import (android\.|androidx\.|com\.app\.miklink\.data\.|com\.app\.miklink\.ui\.|com\.app\.miklink\.di\.)"""),
                message = "core/domain must not import android, androidx, data, ui or di",
                appliesOnlyIfPathContains = "core/domain"
            ),
            // core/data non deve importare Retrofit/Moshi/implementazioni concrete
            ForbiddenPattern(
                regex = Regex("""^import (retrofit2\.|com\.squareup\.moshi\.|com\.app\.miklink\.data\.)"""),
                message = "core/data must not import Retrofit, Moshi or concrete data implementations",
                appliesOnlyIfPathContains = "core/data"
            )
        )

        val violations = mutableListOf<String>()
        val projectDir = layout.projectDirectory.asFile
        roots.get().map { File(projectDir, it) }
            .filter { it.exists() }
            .forEach { root ->
                root.walkTopDown()
                    .filter { it.isFile && it.extension.lowercase() in setOf("kt", "kts", "md", "txt") }
                    .forEach { file ->
                        val relativePath = file.relativeTo(projectDir).toString().replace(File.separatorChar, '/')
                        file.readLines().forEachIndexed { index, line ->
                            patterns.forEach { pattern ->
                                if (pattern.appliesOnlyIfPathContains != null &&
                                    !relativePath.contains(pattern.appliesOnlyIfPathContains)
                                ) {
                                    return@forEach
                                }
                                if (pattern.skipIfPathContains != null &&
                                    relativePath.contains(pattern.skipIfPathContains)
                                ) {
                                    return@forEach
                                }
                                pattern.regex.findAll(line).forEach {
                                    val lineNumber = index + 1
                                    violations.add("$relativePath:$lineNumber -> ${pattern.message}")
                                }
                            }
                        }
                    }
            }

        if (violations.isNotEmpty()) {
            violations.forEach { logger.error(it) }
            throw GradleException("Forbidden patterns found (${violations.size}); see log for details.")
        } else {
            logger.lifecycle("checkForbiddenPatterns: no forbidden patterns found in ${roots.get().joinToString()}.")
        }
    }
}
