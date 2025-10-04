import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    `kotlin-dsl`
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.vanniktech.mavenPublish)
}

kotlin {
    compilerOptions {
        moduleName = "kmp.gen.gradle-plugin"
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

tasks.validatePlugins {
    enableStricterValidation.set(true)
}

dependencies {
    implementation(project(":core"))

    compileOnly(gradleApi())
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.agp)
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()
    coordinates(group.toString(), name, version.toString())

    pom(pomAction)
}

gradlePlugin {
    plugins {
        create("kmpgenPlugin") {
            id = "com.kroegerama.openapi-kmp-gen"
            implementationClass = "com.kroegerama.openapi.kmp.gen.plugin.KgenPlugin"
            displayName = C.PROJECT_NAME

            website.set(C.PROJECT_WEBSITE)
            vcsUrl.set(C.PROJECT_GIT)
            description = C.PROJECT_DESCRIPTION
            tags.set(listOf("openapi", "generator", "codegen", "swagger", "kmp", "kotlin", "ktor"))
        }
    }
}
