package com.kroegerama.openapi.kmp.gen.spec

import com.kroegerama.openapi.kmp.gen.Constants
import com.kroegerama.openapi.kmp.gen.OptionSet
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import java.util.IdentityHashMap

class SpecVisitor(
    private val openAPI: OpenAPI,
    private val options: OptionSet
) {

    /**
     * Visits every schema reachable from the paths. Component schemas are visited when
     * [allComponentSchemas] is set, when they are force-created, or when the
     * `generateAllNamedSchemas` option is enabled.
     */
    fun visit(
        allComponentSchemas: Boolean = false,
        visitor: (schema: Schema<*>) -> Unit
    ) {
        val visited = IdentityHashMap<Any, Any>()

        openAPI.paths?.forEach { (_, pathItem) ->
            visitPathItem(
                visFun = { obj -> visited.put(obj, marker) == null },
                pathItem = pathItem,
                visitor = visitor
            )
        }
        openAPI.components?.schemas?.forEach { (_, schema) ->
                val forceCreate = schema.extensions?.get(Constants.EXT_FORCE_CREATE) as? Boolean == true
                if (allComponentSchemas || forceCreate || options.generateAllNamedSchemas) {
                    visitSchema(
                        visFun = { obj -> visited.put(obj, marker) == null },
                        schema = schema,
                        visitor = visitor
                    )
                }
        }
    }

    private fun visitPathItem(
        visFun: (Any) -> Boolean,
        pathItem: PathItem,
        visitor: (schema: Schema<*>) -> Unit
    ) {
        pathItem.parameters?.forEach { parameter ->
            visitParameter(visFun, parameter, visitor)
        }

        pathItem.readOperations().forEach { operation ->
            visitOperation(visFun, operation, visitor)
        }
    }

    private fun visitOperation(
        visFun: (Any) -> Boolean,
        operation: Operation,
        visitor: (schema: Schema<*>) -> Unit
    ) {
        if (options.limitApis.isNotEmpty()) {
            val tags = operation.resolveTags()
            if (tags.none { it in options.limitApis }) {
                return
            }
        }
        operation.parameters?.forEach { parameter ->
            visitParameter(visFun, parameter, visitor)
        }
        operation.requestBody?.content?.let { content ->
            visitContent(visFun, content, visitor)
        }
        operation.responses?.forEach { (_, response) ->
            visitResponse(visFun, response, visitor)
        }

        //TODO callbacks are not supported right now...
//    operation.callbacks?.forEach { (callbackName, callback) ->
//        callback?.forEach { (pathItemName, pathItem) ->
//            visitPathItem(pathItem,  visitor)
//        }
//    }
    }

    private fun visitParameter(
        visFun: (Any) -> Boolean,
        parameter: Parameter,
        visitor: (schema: Schema<*>) -> Unit
    ) {
        parameter.schema?.let { schema ->
            visitSchema(visFun, schema, visitor)
        }
        parameter.content?.let { content ->
            visitContent(visFun, content, visitor)
        }
    }

    private fun visitContent(
        visFun: (Any) -> Boolean,
        content: Content,
        visitor: (schema: Schema<*>) -> Unit
    ) {
        content.forEach { (mime, mediaType) ->
            mediaType.schema?.let { schema ->
                visitSchema(visFun, schema, visitor)
            }
        }
    }

    private fun visitResponse(
        visFun: (Any) -> Boolean,
        response: ApiResponse,
        visitor: (schema: Schema<*>) -> Unit
    ) {
        val refResponse = response.`$ref`?.substringAfterLast('/')
        if (refResponse != null) {
            openAPI.components?.responses?.get(refResponse)?.let {
                visitResponse(visFun, it, visitor)
            }
        }
        response.content?.let { content ->
            visitContent(visFun, content, visitor)
        }
        response.headers?.forEach { (_, header) ->
            visitHeader(visFun, header, visitor)
        }
    }

    private fun visitHeader(
        visFun: (Any) -> Boolean,
        header: Header,
        visitor: (schema: Schema<*>) -> Unit
    ) {
        header.schema?.let { schema ->
            visitSchema(visFun, schema, visitor)
        }
        header.content?.let { content ->
            visitContent(visFun, content, visitor)
        }
    }

    private fun visitSchema(
        visFun: (Any) -> Boolean,
        schema: Schema<*>,
        visitor: (schema: Schema<*>) -> Unit
    ) {
        if (!visFun(schema)) {
            return
        }
        val refType = schema.`$ref`?.substringAfterLast('/')
        if (refType != null) {
            openAPI.components?.schemas?.get(refType)?.let { refSchema ->
                visitSchema(visFun, refSchema, visitor)
            }
            return
        }

        visitor.invoke(schema)

        schema.oneOf?.forEach { s ->
            visitSchema(visFun, s, visitor)
        }
        schema.allOf?.forEach { s ->
            visitSchema(visFun, s, visitor)
        }
        schema.anyOf?.forEach { s ->
            visitSchema(visFun, s, visitor)
        }
        schema.items?.let { items ->
            visitSchema(visFun, items, visitor)
        }
        (schema.additionalProperties as? Schema<*>)?.let { additionalProperties ->
            visitSchema(visFun, additionalProperties, visitor)
        }
        schema.not?.let { not ->
            visitSchema(visFun, not, visitor)
        }
        schema.properties?.forEach { (_, property) ->
            visitSchema(visFun, property, visitor)
        }
    }

    companion object {
        private val marker = Any()
    }
}
