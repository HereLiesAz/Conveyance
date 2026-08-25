plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())

    // Mobile and tablets are the reason this framework exists at all -- a desktop-only demo never
    // put a gesture, an Escort, or a felt refusal under an actual thumb. This target is what
    // conveyance-demo-android links against; nothing here is desktop-specific, so it costs nothing
    // to share.
    androidLibrary {
        namespace = "com.hereliesaz.conveyance.demo"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(project(":conveyance-compose"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            // The five composable-set libraries, for the style showcase alongside the photograph
            // gallery. Each transitively depends on this same repo's own conveyance-core/-compose
            // artifacts via JitPack; excluded here since `project(":conveyance-compose")` above
            // (which itself depends on `project(":conveyance-core")`) already supplies those exact
            // classes from this build -- without the exclude, both the JitPack jar and the local
            // project would define the identical classes on the same classpath.
            listOf(
                "com.github.HereLiesAz:conveyance-h2g2:main-SNAPSHOT",
                "com.github.HereLiesAz:conveyance-expressive:main-SNAPSHOT",
                "com.github.HereLiesAz:conveyance-liquid:main-SNAPSHOT",
                "com.github.HereLiesAz:conveyance-bacterium:main-SNAPSHOT",
                "com.github.HereLiesAz:conveyance-space:main-SNAPSHOT",
            ).forEach { coordinate ->
                implementation(coordinate) { exclude(group = "com.github.HereLiesAz.Conveyance") }
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":conveyance-auditor"))
                // The auditor keeps Jackson internal, correctly; the harness serialises the bundle
                // itself and so needs its own.
                implementation(libs.jackson.kotlin)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.hereliesaz.conveyance.demo.MainKt"
    }
}

tasks.withType<Test>().configureEach {
    // Rendering happens off-screen; there is no display in CI and none is needed.
    systemProperty("java.awt.headless", "true")
    testLogging { events("passed", "failed", "skipped") }
    // RenderMotion holds every captured frame of a film in memory before writing it out --
    // several dozen full-resolution ARGB frames per test, several films per run. The default
    // heap is sized for ordinary unit tests and starts failing with OutOfMemoryError once enough
    // of them run in the same worker, which is a resource limit, not a defect in what the films
    // are testing.
    maxHeapSize = "2g"
}
