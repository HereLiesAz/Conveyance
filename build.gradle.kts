import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.dokka.gradle.formats.DokkaFormatPlugin
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi

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

// Root-level Markdown aggregation, alongside the HTML aggregation `alias(libs.plugins.dokka)`
// above already gives the root project -- GitHub wikis render Markdown (via Gollum), not a
// static HTML site with its own CSS/JS assets. See .github/workflows/publish-docs.yml.
apply<DokkaMarkdownPlugin>()

// Coordinates every publishable module shares. The version is read from version.properties --
// the same file CI stamps releases from -- so there is exactly one place a version lives.
// JitPack overrides both group and version from the git tag it builds; this is what a local
// `publishToMavenLocal` produces.
val conveyanceVersion: String = java.util.Properties().run {
    rootProject.file("version.properties").inputStream().use { load(it) }
    listOf("versionMajor", "versionMinor", "versionPatch", "versionBuild")
        .joinToString(".") { getProperty(it).trim() }
}

allprojects {
    group = "com.hereliesaz.conveyance"
    version = conveyanceVersion
}

// The KMP Security Pipeline workflow runs `./gradlew detekt`, so the task has to exist at the root
// and reach every module. Without this it fails with "task not found" on every push.
subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    // The demo modules depend on the five composable-set libraries' `main-SNAPSHOT` -- a genuinely
    // moving target on JitPack, rebuilt on every push to those repos' own `main`. Gradle's default
    // cache policy for "-SNAPSHOT"-suffixed ("changing") modules is to trust a resolved version for
    // 24 hours before re-checking, which is exactly wrong here: a CI run minutes after one of those
    // repos pushed a fix would keep resolving the stale version its own cache (or a restored
    // GitHub Actions cache) already had, never seeing the fix land. Forcing a 0-second cache window
    // is the correct policy for a dependency that is deliberately never actually stable.
    if (name == "conveyance-demo" || name == "conveyance-demo-android") {
        configurations.all { resolutionStrategy.cacheChangingModulesFor(0, "seconds") }
    }

    // The demo is an application built to prove the framework, not a piece of the framework's
    // API surface -- there is nothing there a consumer needs a reference for. Its Android launcher
    // is even less of one: an Activity and a manifest, nothing else.
    if (name != "conveyance-demo" && name != "conveyance-demo-android") {
        apply(plugin = "org.jetbrains.dokka")
        // GitHub wikis render Markdown (via Gollum), not a static HTML site with its own CSS/JS
        // assets -- Dokka's default output. This adds GitHub-Flavored-Markdown as an additional
        // output format (`dokkaGenerateMarkdown`, aggregating across subprojects the same way
        // `dokkaGenerateHtml` already does) so the API reference can be pushed straight into the
        // wiki repo -- see .github/workflows/publish-docs.yml.
        apply<DokkaMarkdownPlugin>()
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
    // License matches the LICENSE file at the repo root (Apache 2.0, same as convey/convey-web).
    pluginManager.withPlugin("maven-publish") {
        extensions.configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                pom {
                    url.set("https://github.com/HereLiesAz/Conveyance")
                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }
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

@OptIn(InternalDokkaGradlePluginApi::class)
abstract class DokkaMarkdownPlugin : DokkaFormatPlugin(formatName = "markdown") {
    override fun DokkaFormatPlugin.DokkaFormatPluginContext.configure() {
        project.dependencies {
            dokkaPlugin(dokka("gfm-plugin"))
            formatDependencies.dokkaPublicationPluginClasspathApiOnly.dependencies.addLater(
                dokka("gfm-template-processing-plugin")
            )
        }
    }
}
