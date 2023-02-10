@Suppress("DSL_SCOPE_VIOLATION") // XXX https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    alias(libs.plugins.android.application)
    id(libs.plugins.kotlin.android.get().pluginId)
    alias(libs.plugins.compose)
    base
}

dependencies {
    implementation(project(":compose:common"))
    implementation(project(":kmpclient"))
    implementation(libs.ktor.client.cio)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
}

base.archivesName.set(rootProject.name)

android {
    compileSdk = (project.property("androidCompileSDK") as String).toInt()
    namespace = "${group}.android"
    defaultConfig {
        applicationId = "${group}"
        minSdk = (project.property("androidMinSDK") as String).toInt()
        targetSdk = (project.property("androidTargetSDK") as String).toInt()
        versionCode = 1
        versionName = "1.0-SNAPSHOT"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}
