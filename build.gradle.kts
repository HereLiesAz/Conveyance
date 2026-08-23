// Every plugin is resolved once here, so a subproject's `alias(...)` only applies it.
// Declaring a version in two places is how a build starts needing a manual.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
}
