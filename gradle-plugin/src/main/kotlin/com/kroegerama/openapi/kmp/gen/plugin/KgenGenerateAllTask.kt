package com.kroegerama.openapi.kmp.gen.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory

@CacheableTask
abstract class KgenGenerateAllTask : DefaultTask() {

    @get:OutputDirectory
    abstract val output: DirectoryProperty

}
