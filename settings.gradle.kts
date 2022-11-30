rootProject.name = "mcmapper"

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    versionCatalogs {
        create("libs") {
            version("kotlin", "1.7.20")
            version("kotlinx.serialization", "1.4.0")
            version("compose", "1.3.0-beta03")
            version("johnrengelman.shadow", "7.1.2")
            version("ktor", "2.1.1")

            library("kotlinx.serialization", "org.jetbrains.kotlinx", "kotlinx-serialization-core").versionRef("kotlinx-serialization")
            library("kotlinx.serialization.json", "org.jetbrains.kotlinx", "kotlinx-serialization-json").versionRef("kotlinx-serialization")
            library("ktor.client.core", "io.ktor", "ktor-client-core").versionRef("ktor")
            library("ktor.client.cio", "io.ktor", "ktor-client-cio").versionRef("ktor")
            library("ktor.client.logging", "io.ktor", "ktor-client-logging").versionRef("ktor")
            library("nullicorn.nedit", "me.nullicorn", "Nedit").version("2.2.0")
        }
    }
}

include("backend")

System.getProperty("os.name")?.let { os ->
    if (os == "Linux" || os == "Mac OS X" || os.startsWith("Windows "))
        include("client")
}
