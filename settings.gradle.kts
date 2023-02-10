rootProject.name = "mcmapper"

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
include("backend", "desktop", "mapdata", "kmpclient")

System.getProperty("os.name")?.let { os ->
    if (os == "Linux" || os == "Mac OS X" || os.startsWith("Windows ")) {
        include("compose:common", "compose:js", "compose:jvm", "compose:android")
        for (p in rootProject.children)
            if (p.name == "mapdata" || p.name == "kmpclient")
                p.buildFileName = "build-kmp.gradle.kts"
    }
}
