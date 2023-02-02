import org.jetbrains.compose.desktop.application.dsl.TargetFormat

@Suppress("DSL_SCOPE_VIOLATION") // XXX https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    js(IR) {
        browser {
            distribution {
                directory = File(rootDir, "js")
            }
            runTask {
                outputFileName = "${rootProject.name}.js"
            }
            webpackTask {
                outputFileName = "${rootProject.name}.js"
            }
        }
        binaries.executable()
    }
    sourceSets {
        val commonMain by getting {
            kotlin.srcDirs("../shared/src/kotlin")

            dependencies {
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.runtime)
                implementation(compose.material)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.ktor.client.cio)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(compose.web.core)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "${group}.${rootProject.name}.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "${rootProject.name}-desktop"
            packageVersion = "1.0.0" // XXX apparently versions <1.0 don't exist? version.toString()
        }
    }
}

compose.experimental {
    web.application {}
}
