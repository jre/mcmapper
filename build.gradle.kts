import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

subprojects {
    group = "net.joshe.mcmapper"
    version = "0.1"
}

@Suppress("DSL_SCOPE_VIOLATION") // XXX https://youtrack.jetbrains.com/issue/KTIJ-19369
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

// From https://github.com/ben-manes/gradle-versions-plugin#rejectversionsif-and-componentselection
// via https://github.com/littlerobots/version-catalog-update-plugin#creating-a-libsversionstoml-file
fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

tasks.withType<DependencyUpdatesTask> {
  rejectVersionIf {
    isNonStable(candidate.version) && !isNonStable(currentVersion)
  }
}
