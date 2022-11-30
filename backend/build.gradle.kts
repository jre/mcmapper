import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm").version(libs.versions.kotlin.get())
    kotlin("plugin.serialization").version(libs.versions.kotlinx.serialization.get())
    application
    id("com.github.johnrengelman.shadow").version(libs.versions.johnrengelman.shadow.get())
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.nullicorn.nedit)
}

sourceSets {
    val main by getting {
        kotlin {
            srcDirs("../shared/src/kotlin")
        }
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}

application {
    mainClass.set("${group}.${rootProject.name}.MainKt")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveFileName.set("${rootProject.name}-backend.${archiveExtension.get()}")
    destinationDirectory.set(rootDir)
}
