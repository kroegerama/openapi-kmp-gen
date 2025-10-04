package com.kroegerama.openapi.kmp.gen

data class OptionSet(
    val specFile: String,
    val packageName: String,
    val outputDir: String,
    val limitApis: Set<String>,
    val generateAllNamedSchemas: Boolean,
    val allowParseErrors: Boolean,
    val outputDirIsSrcDir: Boolean,
    val verbose: Boolean
) {

    val apiPackage = "$packageName.api"
    val modelPackage = "$packageName.models"

}
