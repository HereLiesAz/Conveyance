plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            dependencies {
                implementation(project(":conveyance-compose"))
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
