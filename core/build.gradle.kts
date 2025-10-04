import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    compilerOptions {
        moduleName = "kmp.gen.core"
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
    implementation(libs.kotlin.stdlib)
    api(libs.swagger.parser)
    implementation(libs.kotlinpoet)
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

kotlinExtension.sourceSets {
    maybeCreate("main").kotlin {
        srcDir(buildConfigDir)
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
