rootProject.name = "mcmapper"

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

include("backend", "mapdata")

System.getProperty("os.name")?.let { os ->
    if (os == "Linux" || os == "Mac OS X" || os.startsWith("Windows ")) {
        include("client")
        rootProject.children.find { it.name == "mapdata" }?.let { mapdata ->
            mapdata.buildFileName = "build-kmp.gradle.kts"
        }
    }
}
