import org.jetbrains.compose.desktop.application.dsl.TargetFormat

@Suppress("DSL_SCOPE_VIOLATION") // XXX https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
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
        val jsMain by getting {
            dependencies {
                implementation(project(":compose:common"))
                implementation(compose.web.core)
            }
        }
    }
}

compose.experimental {
    web.application {}
}
