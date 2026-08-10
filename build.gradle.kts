plugins {
    alias(libs.plugins.android.application) apply false
    // AGP 9.x has built-in Kotlin support: org.jetbrains.kotlin.android is not applied.
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

val checkForbiddenPatterns by tasks.registering(ForbiddenPatternsTask::class) {
    roots.set(listOf("app/src", "docs"))
}

tasks.register("check") {
    group = "verification"
    description = "Runs repository verification tasks."
    dependsOn(checkForbiddenPatterns, ":app:check")
}
