rootProject.name = "mcmapper"

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

// XXX to work around intellij limitations, you will need to
// comment-out either the backend or client include to open this in
// the ide

include("backend")

System.getProperty("os.name")?.let { os ->
    if (os == "Linux" || os == "Mac OS X" || os.startsWith("Windows "))
        include("client")
}
