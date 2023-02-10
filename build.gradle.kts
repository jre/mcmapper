@file:Suppress("DSL_SCOPE_VIOLATION") // XXX https://youtrack.jetbrains.com/issue/KTIJ-19369

subprojects {
    group = "net.joshe.mcmapper"
    version = "0.1"
}

plugins {
    alias(libs.plugins.benmanes.versions)
    alias(libs.plugins.littlerobots.versioncatalogupdate)
    alias(libs.plugins.kotlin.jvm).apply(false)
    alias(libs.plugins.kotlinx.serialization).apply(false)
    alias(libs.plugins.johnrengelman.shadow).apply(false)

    System.getProperty("os.name")?.let { os ->
        if (os == "Linux" || os == "Mac OS X" || os.startsWith("Windows ")) {
            alias(libs.plugins.kotlin.multiplatform).apply(false)
            alias(libs.plugins.kotlin.android).apply(false)
            alias(libs.plugins.android.application).apply(false)
            alias(libs.plugins.android.library).apply(false)
            alias(libs.plugins.compose).apply(false)
        }
    }
}
