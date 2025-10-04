import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.time.OffsetDateTime

@CacheableTask
abstract class GenerateBuildConfigTask : DefaultTask() {

    @get:Input
    abstract val compose: Property<String>

    @get:Input
    abstract val ktor: Property<String>

    @get:Input
    abstract val companion: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val file = outputDir.get().file("BuildConfig.kt").asFile
        val content = """
            package com.kroegerama.openapi.kmp.gen
            /*
            Generated: ${OffsetDateTime.now()} 
             */
            public object BuildConfig {
                public const val COMPOSE: String = "${compose.get()}"
                public const val COMPANION: String = "${companion.get()}"
                public const val KTOR: String = "${ktor.get()}"
            }
        """.trimIndent()
        file.writeText(content)
    }

}
