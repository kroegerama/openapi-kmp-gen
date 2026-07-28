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
        apiVersion = KotlinVersion.KOTLIN_2_2
        languageVersion = KotlinVersion.KOTLIN_2_2
    }
    coreLibrariesVersion = "2.2.21"
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 11
}

dependencies {
    implementation(projects.core)

    implementation(libs.airline)
    implementation(libs.logback.classic)
    implementation(libs.guava)
}

tasks.shadowJar {
    // transformers (service files, kotlin_module merging) need to see all duplicates;
    // everything else keeps first-wins semantics to avoid duplicate jar entries
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    filesNotMatching(listOf("META-INF/services/**", "META-INF/*.kotlin_module")) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

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
