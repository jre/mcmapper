import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION") // XXX https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    id(libs.plugins.kotlin.multiplatform.get().pluginId)
    alias(libs.plugins.android.library)
}

kotlin {
    android()
    js(IR) {
        browser()
    }
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":mapdata"))
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.logging)
                //testImplementation(kotlin("test"))
            }
        }
    }
}

android {
    compileSdk = (project.property("androidCompileSDK") as String).toInt()
    namespace = "${group}.common"
    defaultConfig {
        minSdk = (project.property("androidMinSDK") as String).toInt()
        targetSdk = (project.property("androidTargetSDK") as String).toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

// XXX java-library sets these but the kotlin multiplatform plugin appears not to
configurations {
    "jvmRuntimeElements" {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 8)
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
}

/* tasks.test {
    useJUnitPlatform()
} */

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
