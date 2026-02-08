package com.kroegerama.openapi.kmp.gen.plugin

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

abstract class KgenExtension {

    abstract val specs: NamedDomainObjectContainer<SpecInfo>

    fun spec(packageName: String, action: Action<SpecInfo>) {
        specs.register(packageName) { specInfo ->
            specInfo.specFile.convention(null as RegularFile?)
            specInfo.specUri.convention(null as String?)
            specInfo.limitApis.convention(emptySet())
            specInfo.generateAllNamedSchemas.convention(false)
            specInfo.allowParseErrors.convention(false)
            specInfo.verbose.convention(false)
            action.execute(specInfo)
        }
    }
}

interface SpecInfo {
    val name: String
    val specFile: RegularFileProperty
    val specUri: Property<String>
    val limitApis: SetProperty<String>
    val generateAllNamedSchemas: Property<Boolean>
    val allowParseErrors: Property<Boolean>
    val verbose: Property<Boolean>
}
