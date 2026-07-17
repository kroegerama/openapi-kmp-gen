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

val generate30 = tasks.register<JavaExec>("generate30") {
    dependsOn(project(":cli").tasks.named("shadowJar"))

    group = "kmpgen"
    description = "Generate from OpenAPI 3.0 spec"
    inputs.files("testspec_30.yaml")

    val shadowJar = fileTree("../cli/build/libs") {
        include("**-$version-shadow.jar")
    }

    classpath = files(shadowJar)
    mainClass = "com.kroegerama.openapi.kmp.gen.cli.CommandLineKt"

    args = listOf(
        "generate",
        "-p", "com.kroegerama.kmp.gen.generated30",
        "-o", "src/commonMain/kotlin",
        "--created-at", "2026-06-01T13:00:00Z",
        "-s",
        "-a",
        "testspec_30.yaml"
    )
}

val generate31 = tasks.register<JavaExec>("generate31") {
    dependsOn(project(":cli").tasks.named("shadowJar"))

    group = "kmpgen"
    description = "Generate from OpenAPI 3.1 spec"
    inputs.files("testspec_31.yaml")

    val shadowJar = fileTree("../cli/build/libs") {
        include("**-$version-shadow.jar")
    }

    classpath = files(shadowJar)
    mainClass = "com.kroegerama.openapi.kmp.gen.cli.CommandLineKt"

    args = listOf(
        "generate",
        "-p", "com.kroegerama.kmp.gen.generated31",
        "-o", "src/commonMain/kotlin",
        "--created-at", "2026-06-01T13:00:00Z",
        "-s",
        "-a",
        "testspec_31.yaml"
    )
}

tasks.register("generate") {
    dependsOn(generate30, generate31)
    group = "kmpgen"
    description = "Generate from OpenAPI specs"
}
