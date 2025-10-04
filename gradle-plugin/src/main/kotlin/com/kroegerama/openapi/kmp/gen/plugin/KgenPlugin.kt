package com.kroegerama.openapi.kmp.gen.plugin

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.crash.afterEvaluate
import com.kroegerama.openapi.kmp.gen.BuildConfig
import com.kroegerama.openapi.kmp.gen.Constants
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

class KgenPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val extension = project.extensions.create<KgenExtension>(Constants.EXTENSION_NAME)
        val outputDirectory = project.layout.buildDirectory.dir(Constants.PLUGIN_BUILD_OUTPUT_PATH)

        val prepareTask = project.tasks.register<KgenPrepareTask>(Constants.TASK_NAME_PREPARE) {
            group = Constants.TASK_GROUP
            description = Constants.TASK_DESCRIPTION
            output.set(outputDirectory)
        }

        val generateAll = project.tasks.register<KgenGenerateAllTask>(Constants.TASK_NAME_PREPARE_ALL) {
            group = Constants.TASK_GROUP
            description = Constants.TASK_DESCRIPTION
            output.set(outputDirectory)
            dependsOn(project.tasks.withType<KgenTask>())
        }

        project.tasks.withType<Jar>().configureEach {
            dependsOn(generateAll)
        }

        project.tasks.withType<KotlinCompile>().configureEach {
            dependsOn(generateAll)
        }

        project.tasks.withType<KotlinNativeCompile>().configureEach {
            dependsOn(generateAll)
        }

        project.tasks.withType<KotlinCompileCommon>().configureEach {
            dependsOn(generateAll)
        }

        extension.specs.all spec@{
            val taskName = Constants.TASK_NAME_PREPARE_PREFIX + name
            project.tasks.register<KgenTask>(taskName) {
                group = Constants.TASK_GROUP
                description = Constants.TASK_DESCRIPTION
                dependsOn(prepareTask)
                setProperties(extension, this@spec, outputDirectory)
            }
        }

        project.pluginManager.withPlugin("idea") {
            project.logger.info("[kmpgen] Configure plugin 'idea'")
            configureIdea(project, outputDirectory)
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            project.logger.info("[kmpgen] Configure plugin 'org.jetbrains.kotlin.jvm'")
            configureKotlinJvm(project, outputDirectory)
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.android") {
            project.logger.info("[kmpgen] Configure plugin 'org.jetbrains.kotlin.android'")
            configureKotlinAndroid(project, outputDirectory)
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            project.logger.info("[kmpgen] Configure plugin 'org.jetbrains.kotlin.multiplatform'")
            configureKMP(project, outputDirectory)
        }

        project.pluginManager.withPlugin("com.android.base") {
            project.logger.info("[kmpgen] Configure plugin 'com.android.base'")
            configureAndroid(project, generateAll, outputDirectory)
        }

        project.afterEvaluate {
            val hasSerializationPlugin = project.pluginManager.hasPlugin("org.jetbrains.kotlin.plugin.serialization")
            if (!hasSerializationPlugin) {
                throw GradleException(
                    "Kotlin Serialization plugin missing: " +
                            "'org.jetbrains.kotlin.plugin.serialization' must be added to the project '${project.name}'"
                )
            }
        }
    }

    private fun configureIdea(project: Project, outputDirectory: Provider<Directory>) {
        project.extensions.configure<IdeaModel> {
            project.logger.info("[kmpgen] Configure IdeaModel, add generated sources to generatedSourceDirs")
            module {
                generatedSourceDirs.add(outputDirectory.get().asFile)
            }
        }
    }

    private fun configureKotlinJvm(project: Project, outputDirectory: Provider<Directory>) {
        project.extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                optIn.add("kotlin.uuid.ExperimentalUuidApi")
                optIn.add("kotlin.time.ExperimentalTime")
                optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }

        project.extensions.configure<KotlinSourceSetContainer> {
            sourceSets.named("main") {
                project.logger.info("[kmpgen] Configure Jvm SourceSetContainer 'sourceSets', add generated sources to 'main' srcDir")
                kotlin.srcDir(outputDirectory)
            }
        }

        project.dependencies {
            project.logger.info("[kmpgen] Add companion dependency to jvm project")
            add("api", "com.kroegerama.openapi-kmp-gen:companion:${BuildConfig.COMPANION}")
        }
    }

    private fun configureKotlinAndroid(project: Project, outputDirectory: Provider<Directory>) {
        project.extensions.configure<KotlinAndroidProjectExtension> {
            compilerOptions {
                optIn.add("kotlin.uuid.ExperimentalUuidApi")
                optIn.add("kotlin.time.ExperimentalTime")
                optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }

        project.dependencies {
            project.logger.info("[kmpgen] Add companion dependency to android project")
            add("api", "com.kroegerama.openapi-kmp-gen:companion:${BuildConfig.COMPANION}")
        }
    }

    private fun configureKMP(project: Project, outputDirectory: Provider<Directory>) {
        project.extensions.configure<KotlinMultiplatformExtension> {
            compilerOptions {
                optIn.add("kotlin.uuid.ExperimentalUuidApi")
                optIn.add("kotlin.time.ExperimentalTime")
                optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }
        project.extensions.configure<KotlinSourceSetContainer>("kotlin") {
            project.logger.info("[kmpgen] Configure KotlinSourceSetContainer 'kotlin', add generated sources to 'commonMain' srcDir")
            sourceSets.named("commonMain") {
                kotlin.srcDir(outputDirectory)
                dependencies {
                    api("com.kroegerama.openapi-kmp-gen:companion:${BuildConfig.COMPANION}")
                }
            }
        }
    }

    private fun configureAndroid(project: Project, generateAllTask: TaskProvider<KgenGenerateAllTask>, outputDirectory: Provider<Directory>) {
        project.extensions.findByType(BaseExtension::class.java)?.apply {
            sourceSets {
                project.logger.info("[kmpgen] Configure AndroidSourceSet, add generated sources to 'main' srcDir")
                named("main") {
                    kotlin.srcDir(outputDirectory)
                }
            }
        }
//        project.extensions.findByType(AppExtension::class.java)?.apply {
//            applicationVariants.configureEach {
//                project.logger.info("[kmpgen] Configure AppExtension for applicationVariant '$name', add generated sources")
//                registerJavaGeneratingTask(generateAllTask, outputDirectory.get().asFile)
//            }
//        }
//        project.extensions.findByType(LibraryExtension::class.java)?.apply {
//            libraryVariants.configureEach {
//                project.logger.info("[kmpgen] Configure LibraryExtension for libraryVariant '$name', add generated sources")
//                registerJavaGeneratingTask(generateAllTask, outputDirectory.get().asFile)
//            }
//        }
    }
}
