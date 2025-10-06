import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.vanniktech.mavenPublish)
}

tasks.withType<JavaCompile>().configureEach {
    if ("compileJvm" in name) {
        options.release = 11
    }
}

kotlin {
    explicitApi()

    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_1
        languageVersion = KotlinVersion.KOTLIN_2_1

        optIn.add("kotlin.contracts.ExperimentalContracts")
        optIn.add("kotlin.io.encoding.ExperimentalEncodingApi")
    }
    coreLibrariesVersion = "2.1.21"

    jvm {
        compilerOptions {
            freeCompilerArgs.add("-Xjdk-release=11")
            moduleName = "kmp.gen.companion"
            jvmTarget = JvmTarget.JVM_11
        }
    }
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            moduleName = "kmp.gen.companion"
            jvmTarget = JvmTarget.JVM_11
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()
    mingwX64()
    linuxX64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
            api(libs.ktor.client.core)
            api(libs.ktor.client.content.negotiation)
            api(libs.ktor.serialization.kotlinx.json)
            api(libs.ktor.client.logging)
            api(libs.arrow.core)
            api(libs.compose.runtime.annotation)
        }
        androidMain.dependencies {
            api(libs.ktor.client.okhttp)
            api(libs.kotlinx.coroutines.android)
        }
        jvmMain.dependencies {
            api(libs.ktor.client.okhttp)
        }
        appleMain.dependencies {
            api(libs.ktor.client.darwin)
        }
        linuxMain.dependencies {
            api(libs.ktor.client.curl)
        }
        mingwMain.dependencies {
            api(libs.ktor.client.winhttp)
        }
    }
}

android {
    namespace = "com.kroegerama.openapi.kmp.gen.companion"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()
    coordinates(group.toString(), name, version.toString())

    pom(pomAction)
}

val buildConfigDir = layout.buildDirectory.dir("generated/buildConfig")
pluginManager.withPlugin("idea") {
    extensions.configure<IdeaModel> {
        module {
            generatedSourceDirs.add(buildConfigDir.get().asFile)
        }
    }
}

extensions.configure<KotlinSourceSetContainer>("kotlin") {
    sourceSets.named("commonMain") {
        kotlin.srcDir(buildConfigDir)
    }
}

val generateBuildConfig = tasks.register<GenerateBuildConfigTask>("generateBuildConfig") {
    outputDir = buildConfigDir
    compose = libs.versions.compose
    ktor = libs.versions.ktor
    companion = project.version.toString()
}

tasks.withType<Jar>().configureEach {
    dependsOn(generateBuildConfig)
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateBuildConfig)
}

tasks.withType<KotlinNativeCompile>().configureEach {
    dependsOn(generateBuildConfig)
}

tasks.withType<KotlinCompileCommon>().configureEach {
    dependsOn(generateBuildConfig)
}
