import io.gitlab.arturbosch.detekt.extensions.DetektExtension

// Every plugin is resolved once here, so a subproject's `alias(...)` only applies it.
// Declaring a version in two places is how a build starts needing a manual.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt)
}

// The KMP Security Pipeline workflow runs `./gradlew detekt`, so the task has to exist at the root
// and reach every module. Without this it fails with "task not found" on every push.
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<DetektExtension> {
        parallel = true
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        // A rule violation is a report, not a broken build. The Conscience is the layer that
        // blocks, and it blocks on the framework's own laws rather than on style.
        ignoreFailures = true
        basePath = rootProject.projectDir.absolutePath
    }
}
