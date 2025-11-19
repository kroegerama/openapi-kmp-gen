import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    compilerOptions {
        optIn.add("kotlin.uuid.ExperimentalUuidApi")
        optIn.add("kotlin.time.ExperimentalTime")
        optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
    }

    jvm()

    android {
        namespace = "com.kroegerama.kmp.gen.generated"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        enableCoreLibraryDesugaring = true

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    mingwX64()
    linuxX64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":companion"))
        }
        androidMain.dependencies {
            implementation(libs.lsf4j.api)
            implementation(libs.logback.android)
        }
        jvmMain.dependencies {
            implementation(libs.logback.classic)
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
}

tasks.register<JavaExec>("generate") {
    dependsOn(project(":cli").tasks.named("shadowJar"))

    group = "kmpgen"
    inputs.files("testspec.yaml")

    val shadowJar = fileTree("../cli/build/libs") {
        include("**-shadow.jar")
    }.singleFile

    classpath = files(shadowJar)
    mainClass = "com.kroegerama.openapi.kmp.gen.cli.CommandLineKt"

    args = listOf(
        "generate",
        "-p", "com.kroegerama.kmp.gen.generated",
        "-o", "src/commonMain/kotlin",
        "-s",
        "testspec.yaml"
    )
}
