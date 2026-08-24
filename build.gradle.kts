import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

// Every plugin is resolved once here, so a subproject's `alias(...)` only applies it.
// Declaring a version in two places is how a build starts needing a manual.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
}

// Coordinates every publishable module shares. A version bump or a group rename happens once,
// here, rather than drifting across four build files that each remembered it differently.
allprojects {
    group = "com.hereliesaz.conveyance"
    version = "0.1.0"
}

// The KMP Security Pipeline workflow runs `./gradlew detekt`, so the task has to exist at the root
// and reach every module. Without this it fails with "task not found" on every push.
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    // The demo is an application built to prove the framework, not a piece of the framework's
    // API surface -- there is nothing there a consumer needs a reference for. Its Android launcher
    // is even less of one: an Activity and a manifest, nothing else.
    if (name != "conveyance-demo" && name != "conveyance-demo-android") {
        apply(plugin = "org.jetbrains.dokka")
    }

    extensions.configure<DetektExtension> {
        parallel = true
        buildUponDefaultConfig = true
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        // A rule violation is a report, not a broken build. The Conscience is the layer that
        // blocks, and it blocks on the framework's own laws rather than on style.
        ignoreFailures = true
        basePath = rootProject.projectDir.absolutePath
    }

    // The POM metadata every publishable module shares. A module opts in by applying
    // maven-publish and registering its own publication(s); what that publication says about the
    // project as a whole -- source repository, who to credit -- is answered once, here, rather
    // than by four build files that each had to remember to say the same thing.
    //
    // No `licenses { }` block: there is no LICENSE file in this repository yet. A POM that claims
    // a license the repo doesn't actually carry is a worse state than a POM that says nothing --
    // the license is the project owner's call to make, not a default this build should assume.
    pluginManager.withPlugin("maven-publish") {
        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                pom {
                    url.set("https://github.com/HereLiesAz/Conveyance")
                    developers {
                        developer {
                            id.set("HereLiesAz")
                            name.set("HereLiesAz")
                            url.set("https://github.com/HereLiesAz")
                        }
                    }
                    scm {
                        url.set("https://github.com/HereLiesAz/Conveyance")
                        connection.set("scm:git:https://github.com/HereLiesAz/Conveyance.git")
                        developerConnection.set("scm:git:ssh://git@github.com/HereLiesAz/Conveyance.git")
                    }
                }
            }
        }
    }
}
