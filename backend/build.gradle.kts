import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION") // XXX https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
    application
    alias(libs.plugins.johnrengelman.shadow)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.nullicorn.nedit)
    implementation(libs.ktor.client.core)
    testImplementation(kotlin("test"))
}

sourceSets {
    val main by getting {
        kotlin {
            srcDirs("../shared/src/kotlin")
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
