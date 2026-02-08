package com.kroegerama.openapi.kmp.gen.plugin

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.gradle.internal.tasks.factory.dependsOn
import com.kroegerama.openapi.kmp.gen.BuildConfig
import com.kroegerama.openapi.kmp.gen.Constants
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.HasConfigurableKotlinCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

class KgenPlugin : Plugin<Project> {

    private fun Project.log(msg: String) {
        logger.info(msg)
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create(Constants.EXTENSION_NAME, KgenExtension::class.java)
        val outputDirectory = project.layout.buildDirectory.dir(Constants.PLUGIN_BUILD_OUTPUT_PATH)

        val prepareTask = project.tasks.register(Constants.TASK_NAME_PREPARE, KgenPrepareTask::class.java) { task ->
            task.group = Constants.TASK_GROUP
            task.description = Constants.TASK_DESCRIPTION
            task.output.set(outputDirectory)
        }

        val generateAll = project.tasks.register(Constants.TASK_NAME_PREPARE_ALL, KgenGenerateAllTask::class.java) { task ->
            task.group = Constants.TASK_GROUP
            task.description = Constants.TASK_DESCRIPTION
            task.output.set(outputDirectory)
        }

        extension.specs.all spec@{ specInfo ->
            val taskName = Constants.TASK_NAME_PREPARE_PREFIX + specInfo.name
            val task = project.tasks.register(taskName, KgenTask::class.java) { task ->
                task.group = Constants.TASK_GROUP
                task.description = Constants.TASK_DESCRIPTION
                task.dependsOn(prepareTask)
                task.setProperties(extension, specInfo, outputDirectory)
            }
            generateAll.dependsOn(task)
        }

        project.tasks.withType(Jar::class.java).configureEach {
            it.dependsOn(generateAll)
        }

        project.tasks.withType(KotlinCompile::class.java).configureEach {
            it.dependsOn(generateAll)
        }

        project.tasks.withType(KotlinNativeCompile::class.java).configureEach {
            it.dependsOn(generateAll)
        }

        project.tasks.withType(KotlinCompileCommon::class.java).configureEach {
            it.dependsOn(generateAll)
        }

        project.pluginManager.withPlugin("idea") {
            project.log("configure 'idea'")
            project.configureIdea(outputDirectory)
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            project.log("configure 'org.jetbrains.kotlin.jvm'")
            project.configureKotlinSourceSetContainers(outputDirectory)
        }

        project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            project.log("configure 'org.jetbrains.kotlin.multiplatform'")
            project.configureKotlinSourceSetContainers(outputDirectory)
        }

        project.pluginManager.withPlugin("com.android.base") {
            project.log("configure 'com.android.base'")
            project.configureAndroidComponents(generateAll)
        }

        project.configureCompilerOptions()

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

    private fun Project.configureIdea(
        outputDirectory: Provider<Directory>
    ) {
        extensions.configure(IdeaModel::class.java) { model ->
            log("configure IdeaModel, add generated sources to generatedSourceDirs")
            model.module.generatedSourceDirs.add(outputDirectory.get().asFile)
        }
    }

    private fun Project.configureCompilerOptions() {
        extensions.configure(HasConfigurableKotlinCompilerOptions::class.java) { configurable ->
            log("configure KotlinCompilerOptions ${configurable.javaClass.simpleName}")
            configurable.compilerOptions {
                optIn.add("kotlin.uuid.ExperimentalUuidApi")
                optIn.add("kotlin.time.ExperimentalTime")
                optIn.add("kotlinx.serialization.ExperimentalSerializationApi")
            }
        }
    }

    private fun Project.configureKotlinSourceSetContainers(
        outputDirectory: Provider<Directory>
    ) {
        extensions.configure(KotlinSourceSetContainer::class.java) { container ->
            container.sourceSets.named(sourceSetNames::contains).configureEach { sourceSet ->
                log("configure KotlinSourceSetContainer '${sourceSet.name}'")

                log("\tadd generated sources to srcDir")
                sourceSet.kotlin.srcDir(outputDirectory)

                log("\tadd companion dependency version ${BuildConfig.COMPANION} to SourceSet")
                sourceSet.dependencies {
                    api("com.kroegerama.openapi-kmp-gen:companion:${BuildConfig.COMPANION}")
                }
            }
        }
    }

    private fun Project.configureAndroidComponents(
        generateAllTask: TaskProvider<KgenGenerateAllTask>
    ) {
        extensions.configure(AndroidComponentsExtension::class.java) { extension ->
            log("configure AndroidComponentsExtension ${extension.pluginVersion}")
            val isAtLeastAGP9 = extension.pluginVersion.major >= 9

            extension.onVariants { variant ->
                log("\tadd generated sources to variant '${variant.name}'")
                if (isAtLeastAGP9) {
                    variant.sources.kotlin
                } else {
                    variant.sources.java
                }!!.addGeneratedSourceDirectory(
                    generateAllTask,
                    KgenGenerateAllTask::output
                )
            }
        }

        log("add companion dependency version ${BuildConfig.COMPANION} to project '$name'")
        dependencies.add("api", "com.kroegerama.openapi-kmp-gen:companion:${BuildConfig.COMPANION}")
    }

    companion object {
        private val sourceSetNames = listOf("main", "commonMain")
    }
}
