plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
    compilerOptions {
        // The core carries no UI toolkit, no platform, and no runtime dependency of any kind.
        // If a `dependencies { implementation(...) }` line ever appears below, the model has
        // stopped being portable and the framework has stopped being a model.
        allWarningsAsErrors.set(true)
    }
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "conveyance-core"
            pom {
                name.set("Conveyance Core")
                description.set(
                    "The type system: acts, consequences, gates, places and the rules that make " +
                        "an illegal interface state unrepresentable, with no UI toolkit dependency.",
                )
            }
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "failed", "skipped") }
}
