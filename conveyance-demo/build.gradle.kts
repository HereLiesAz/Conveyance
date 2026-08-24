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
}
