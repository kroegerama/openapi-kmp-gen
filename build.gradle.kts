import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
    alias(libs.plugins.versions)
}

allprojects {
    version = C.PROJECT_VERSION
    group = C.PROJECT_GROUP_ID
    description = C.PROJECT_DESCRIPTION
}

tasks.withType<DependencyUpdatesTask>().configureEach {
    gradleReleaseChannel = "current"
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

private val nonStableQualifiers = listOf("alpha", "beta", "rc")

private fun isNonStable(version: String): Boolean = nonStableQualifiers.any { qualifier ->
    qualifier in version.lowercase()
}
