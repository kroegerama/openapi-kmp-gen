package com.kroegerama.openapi.kmp.gen

object Constants {
    const val CLI_NAME = "openapi-kmpgen"
    const val DEFAULT_PACKAGE_NAME = "com.kroegerama.kmp.gen.generated"

    const val MIME_TYPE_JSON = "application/json"
    const val MIME_TYPE_MULTIPART_FORM_DATA = "multipart/form-data"
    const val MIME_TYPE_URL_ENCODED = "application/x-www-form-urlencoded"

    const val FALLBACK_TAG = "default"

    const val SRC_PATH = "src/main/kotlin"

    const val FILE_HEADER_NOTE = "NOTE: This file is auto generated. Do not edit the file manually!"

    const val TASK_GROUP = "kmpgen"
    const val TASK_DESCRIPTION = "Generates source files from the OpenAPI Spec"
    const val EXTENSION_NAME = "kmpgen"
    const val TASK_NAME_PREPARE = "kmpgenPrepare"
    const val TASK_NAME_PREPARE_ALL = "kmpgenGenerateAll"
    const val TASK_NAME_PREPARE_PREFIX = "kmpgenGenerate_"
    const val PLUGIN_BUILD_OUTPUT_PATH = "generated/kmpgen"

    const val EXT_FORCE_CREATE = "x-kgen-force-create"

    val generatorInfo: String = "OpenAPI KMP Gen (version %s) by kroegerama".format(BuildConfig.COMPANION)
}
