pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "conveyance"

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        // The five composable-set libraries (conveyance-h2g2/-expressive/-liquid/-bacterium/-space)
        // are separate repos with no tagged release yet, resolved against `main` here the same way
        // they themselves resolve this repo's own artifacts.
        maven("https://jitpack.io")
    }
}

include(":conveyance-core")
include(":conveyance-compose")
include(":conveyance-demo")
include(":conveyance-demo-android")
include(":conveyance-auditor")
