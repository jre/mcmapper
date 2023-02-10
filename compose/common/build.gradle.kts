@Suppress("DSL_SCOPE_VIOLATION") // XXX https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    android()
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    js(IR) {
        browser()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":mapdata"))
                implementation(project(":kmpclient"))
                api(compose.ui)
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    compileSdk = (project.property("androidCompileSDK") as String).toInt()
    namespace =  "${group}.ui"
    defaultConfig {
        minSdk = (project.property("androidMinSDK") as String).toInt()
        targetSdk = (project.property("androidTargetSDK") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
