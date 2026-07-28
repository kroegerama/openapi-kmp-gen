import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
}

allprojects {
    version = C.PROJECT_VERSION
    group = C.PROJECT_GROUP_ID
    description = C.PROJECT_DESCRIPTION

    plugins.withId("com.vanniktech.maven.publish") {
        configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            signAllPublications()
            coordinates(
                groupId = group.toString(),
                version = version.toString()
            )
            pom(pomAction)
        }
    }
}
