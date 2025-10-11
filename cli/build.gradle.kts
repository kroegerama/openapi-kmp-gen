import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.shadow)
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    compilerOptions {
        moduleName = "kmp.gen.cli"
        jvmTarget = JvmTarget.JVM_11
        freeCompilerArgs.add("-Xjdk-release=11")
        apiVersion = KotlinVersion.KOTLIN_2_1
        languageVersion = KotlinVersion.KOTLIN_2_1
    }
    coreLibrariesVersion = "2.1.21"
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 11
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlin.stdlib)

    implementation(libs.airline)
    implementation(libs.logback.classic)
    implementation(libs.guava)
}

tasks.shadowJar {
    archiveBaseName = "openapi-kmp-gen-cli"
    archiveClassifier = "shadow"

    mergeServiceFiles()
    manifest {
        attributes(
            mapOf(
                "Main-Class" to "com.kroegerama.openapi.kmp.gen.cli.CommandLineKt"
            )
        )
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

mavenPublishing {
    coordinates(
        artifactId = name
    )
}
