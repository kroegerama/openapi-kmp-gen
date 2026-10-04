package com.kroegerama.openapi.kmp.gen.spec

import com.kroegerama.openapi.kmp.gen.Logger
import com.kroegerama.openapi.kmp.gen.OptionSet
import io.swagger.parser.OpenAPIParser
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import java.util.IdentityHashMap

private val mimeTokenSeparatorRegex = "[^a-z0-9]+".toRegex()

class SpecParser(
    private val specFile: String,
    private val options: OptionSet,
    private val logger: Logger
) {
    fun parseAndResolve(): OpenAPI {
        val result = OpenAPIParser().readLocation(specFile, emptyList(), SpecConfig.parseOptions)
        if (!options.allowParseErrors && (!result.messages.isNullOrEmpty() || result.openAPI == null)) {
            result.messages?.forEach { message ->
                logger.error(message)
            }
            throw IllegalStateException("Cannot parse spec $specFile")
        }

        result.openAPI.flattenPaths()
        result.openAPI.filterOperations()
        result.openAPI.filterSchemas()
        result.openAPI.components?.schemas?.forEach { (name, schema) ->
            schema.name = name
        }
        return result.openAPI
    }

    private fun OpenAPI.flattenPaths() {
        paths?.forEach { (path, pathItem) ->
            val names = path.trim('/').split('/').joinToString(".") { part ->
                part.trim('{', '}')
            }
            flattenPathItem(
                baseName = names,
                pathItem = pathItem
            )
        }
    }

    private fun OpenAPI.flattenPathItem(baseName: String, pathItem: PathItem) {
        pathItem.parameters?.forEach { parameter ->
            flattenParameter(
                baseName = baseName,
                parameter = parameter
            )
        }
        pathItem.readOperationsMap()?.forEach { (method, operation) ->
            val name = operation.operationId ?: operation.run { "$method.$baseName" }
            operation.parameters?.forEach { parameter ->
                flattenParameter(name, parameter)
            }
            val operationNames = mutableSetOf<String>()
            operation.requestBody?.content?.forEach { (mimeType, mediaType) ->
                flattenMediaType(baseName, "request", method, mimeType, mediaType, operationNames)
            }
            operation.responses?.forEach { (code, apiResponse) ->
                apiResponse.content?.forEach { (mimeType, mediaType) ->
                    flattenMediaType(baseName, "$code.response", method, mimeType, mediaType, operationNames)
                }
            }
        }
    }

    private fun OpenAPI.flattenMediaType(
        baseName: String,
        suffix: String,
        method: PathItem.HttpMethod,
        mimeType: String,
        mediaType: MediaType,
        operationNames: MutableSet<String>
    ) {
        mediaType.schema?.let { schema ->
            val type = schema.effectiveSchema().getSpecType()
            if (type.needsName) {
                val plainName = "$baseName.$suffix"
                val mimeToken = mimeToken(mimeType)
                val methodName = method.name.lowercase() + "." + baseName
                val firstFallback = if (createSchemaName(plainName) in operationNames) "$baseName.$mimeToken.$suffix" else "$methodName.$suffix"
                val name = hoistSchema(schema, listOf(plainName, firstFallback, "$methodName.$mimeToken.$suffix"))
                operationNames += name
                mediaType.schema = Schema<Any>().`$ref`("#/components/schemas/$name")
            }
        }
    }

    private fun OpenAPI.flattenParameter(baseName: String, parameter: Parameter) {
        parameter.schema?.let { schema ->
            val type = schema.effectiveSchema().getSpecType()
            if (type.needsName) {
                val plainName = baseName + "." + parameter.name
                val name = hoistSchema(schema, listOf(plainName, plainName + "." + parameter.`in`))
                parameter.schema = Schema<Any>().`$ref`("#/components/schemas/$name")
            }
        }
    }

    /**
     * Registers [schema] under the first of [baseNames] that is free or already holds an equal schema, falling back to a numeric suffix.
     */
    private fun OpenAPI.hoistSchema(schema: Schema<*>, baseNames: List<String>): String {
        val names = baseNames.map { createSchemaName(it) }
        val candidates = names.asSequence() + generateSequence(2) { it + 1 }.map { names.first() + it }
        for (candidate in candidates) {
            val existing = components?.schemas?.get(candidate)
            if (existing == null) {
                schema(candidate, schema)
                return candidate
            }
            if (existing == schema) {
                return candidate
            }
        }
        error("No schema name available for ${names.first()}")
    }

    private fun mimeToken(mimeType: String): String {
        return when (val subtype = mimeType.substringBefore(';').substringAfter('/').trim().lowercase()) {
            "json" -> "json"
            "x-www-form-urlencoded" -> "form"
            "form-data" -> "multipart"
            else -> subtype.split(mimeTokenSeparatorRegex).joinToString(".")
        }
    }

    private fun createSchemaName(baseName: String) = baseName.split('.').joinToString("") { part ->
        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun OpenAPI.filterOperations() {
        if (options.limitApis.isEmpty()) {
            return
        }
        val filteredPathsMap = paths?.filterValues { pathItem ->
            fun filterOperation(operation: Operation, clear: () -> Unit) {
                val tags = operation.resolveTags()
                if (tags.none { it in options.limitApis }) {
                    clear()
                }
            }
            pathItem.get?.let { operation -> filterOperation(operation) { pathItem.get = null } }
            pathItem.put?.let { operation -> filterOperation(operation) { pathItem.put = null } }
            pathItem.post?.let { operation -> filterOperation(operation) { pathItem.post = null } }
            pathItem.delete?.let { operation -> filterOperation(operation) { pathItem.delete = null } }
            pathItem.options?.let { operation -> filterOperation(operation) { pathItem.options = null } }
            pathItem.head?.let { operation -> filterOperation(operation) { pathItem.head = null } }
            pathItem.patch?.let { operation -> filterOperation(operation) { pathItem.patch = null } }
            pathItem.trace?.let { operation -> filterOperation(operation) { pathItem.trace = null } }

            pathItem.readOperations().isNotEmpty()
        }.orEmpty()
        paths = Paths().apply { putAll(filteredPathsMap) }
    }

    private fun OpenAPI.filterSchemas() {
        if (options.generateAllNamedSchemas) {
            return
        }
        val visitor = SpecVisitor(
            openAPI = this,
            options = options
        )

        val visited = IdentityHashMap<Any, Any>()
        val marker = Any()
        visitor.visit { schema ->
            visited[schema] = marker
        }
        val removeSchemas = mutableSetOf<String>()
        components?.schemas?.forEach { (name, schema) ->
            if (visited[schema] !== marker) {
                removeSchemas += name
            }
        }
        removeSchemas.forEach { name ->
            logger.info("remove unused schema '$name'")
            components?.schemas?.remove(name)
        }
    }
}
