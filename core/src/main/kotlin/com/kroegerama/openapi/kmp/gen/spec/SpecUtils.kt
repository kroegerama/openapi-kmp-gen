package com.kroegerama.openapi.kmp.gen.spec

import com.kroegerama.openapi.kmp.gen.Constants
import com.kroegerama.openapi.kmp.gen.asBaseUrl
import com.kroegerama.openapi.kmp.gen.language.asTypeName
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.servers.Server
import java.util.Collections
import java.util.IdentityHashMap

fun Schema<*>.getSpecType(): SpecSchemaType {
    if (`$ref` != null) return SpecSchemaType.Ref
    if (additionalProperties == true) return SpecSchemaType.Raw
    if (additionalProperties is Schema<*>) return SpecSchemaType.Map
    val resolvedType = resolveType()

    if (resolvedType == "array") return SpecSchemaType.Array
    if (resolvedType == "string" && !enum.isNullOrEmpty()) return SpecSchemaType.Enum

    if (resolvedType in primitiveTypes) {
        val primitiveType = when (resolvedType) {
            "integer" -> when (format) {
                "int32" -> SpecPrimitiveType.Int32
                "int64" -> SpecPrimitiveType.Int64
                "float" -> SpecPrimitiveType.Float // technically not allowed, included to improve compatibility
                "double" -> SpecPrimitiveType.Double // technically not allowed, included to improve compatibility
                "epoch-seconds" -> SpecPrimitiveType.EpochSeconds
                "epoch-millis" -> SpecPrimitiveType.EpochMilliseconds
                else -> SpecPrimitiveType.Int64
            }

            "number" -> when (format) {
                "float" -> SpecPrimitiveType.Float
                "double" -> SpecPrimitiveType.Double
                "int32" -> SpecPrimitiveType.Int32 // technically not allowed, included to improve compatibility
                "int64" -> SpecPrimitiveType.Int64 // technically not allowed, included to improve compatibility
                else -> SpecPrimitiveType.Double
            }

            "string" -> when (format) {
                "date" -> SpecPrimitiveType.Date
                "time" -> SpecPrimitiveType.Time
                "date-time" -> SpecPrimitiveType.DateTime
                "base64" -> SpecPrimitiveType.Base64
                "byte" -> SpecPrimitiveType.Base64
                "uuid" -> SpecPrimitiveType.UUID
                "duration" -> SpecPrimitiveType.Duration
                else -> SpecPrimitiveType.String
            }

            "boolean" -> SpecPrimitiveType.Boolean

            else -> throw IllegalStateException("type $resolvedType in $primitiveTypes, but case is missing")
        }
        return SpecSchemaType.Primitive(
            type = primitiveType
        )
    }

    return when {
        // nullable wrapper with sibling keywords: variant and siblings merge into one object
        singleNonNullVariant() != null -> SpecSchemaType.Object
        oneOf.orEmpty().any { !it.isNullType() } -> SpecSchemaType.Sealed
        anyOf.orEmpty().any { !it.isNullType() } -> SpecSchemaType.Object
        !allOf.isNullOrEmpty() -> SpecSchemaType.Object
        properties.isNullOrEmpty() -> SpecSchemaType.Raw
        else -> SpecSchemaType.Object
    }
}

/**
 * Unwraps composition wrappers that do not describe a type of their own: `oneOf`/`anyOf`
 * with a single non-`null` variant, and `allOf` with a single `$ref`. Sibling keywords that
 * shape the type (`properties`, `additionalProperties`, `enum`, other combinators) prevent
 * unwrapping. Returns the innermost wrapped schema, or this schema if it is not a wrapper.
 * Nullability of removed `null` variants is not carried over; use [isNullable] to detect it.
 */
tailrec fun Schema<*>.effectiveSchema(): Schema<*> {
    val wrapped = wrappedSchema() ?: return this
    return wrapped.effectiveSchema()
}

private fun Schema<*>.wrappedSchema(): Schema<*>? {
    if (`$ref` != null) return null
    if (!properties.isNullOrEmpty()) return null
    if (additionalProperties == true || additionalProperties is Schema<*>) return null
    if (!enum.isNullOrEmpty()) return null
    if (listOfNotNull(oneOf, anyOf, allOf).size != 1) return null
    allOf?.let { members ->
        return members.singleOrNull()?.takeIf { it.`$ref` != null }
    }
    return singleNonNullVariant()
}

/**
 * The single `oneOf`/`anyOf` variant remaining after removing `null` type variants,
 * or `null` if the variants do not have that shape. Sibling keywords are ignored.
 */
private fun Schema<*>.singleNonNullVariant(): Schema<*>? {
    val variants = oneOf ?: anyOf ?: return null
    val nonNullVariants = variants.filterNot { it.isNullType() }
    if (nonNullVariants.size == variants.size) return null
    return nonNullVariants.singleOrNull()
}

/**
 * Whether a value described by this schema may be `null`.
 *
 * An explicit `nullable` attribute wins. Otherwise the schema is nullable when it declares
 * a `null` type (`"null"` in the type list or a `null` variant in `oneOf`/`anyOf`), directly
 * or in a referenced/wrapped schema. If neither applies, a non-[required] value is nullable.
 */
fun Schema<*>.isNullable(spec: OpenAPI, required: Boolean?): Boolean {
    nullable?.let { return it }
    if (isIntrinsicallyNullable(spec)) return true
    if (required != null) return !required
    return false
}

private fun Schema<*>.isIntrinsicallyNullable(spec: OpenAPI): Boolean {
    val visited = Collections.newSetFromMap(IdentityHashMap<Schema<*>, Boolean>())
    var current: Schema<*> = this
    while (visited.add(current)) {
        if (current.nullable == true) return true
        if (current.types?.contains("null") == true) return true
        if (current.oneOf.orEmpty().any { it.isNullType() }) return true
        if (current.anyOf.orEmpty().any { it.isNullType() }) return true
        current = current.resolveRef(spec)
            ?: current.effectiveSchema().takeIf { it !== current }
            ?: return false
    }
    return false
}

fun Schema<*>.isNullType(): Boolean =
    `$ref` == null && (type == "null" || types?.singleOrNull() == "null")

/**
 * Fails generation when this schema is a nullable wrapper with sibling keywords whose single
 * non-null variant is not an object: primitives, enums, arrays and maps cannot be merged into
 * an object with the sibling properties. Schemas without such a variant pass unchanged.
 */
fun Schema<*>.requireMergeableVariant(spec: OpenAPI, name: () -> String) {
    val variant = singleNonNullVariant() ?: return
    val visited: MutableSet<Schema<*>> = Collections.newSetFromMap(IdentityHashMap())
    var terminal = variant.effectiveSchema()
    while (terminal.`$ref` != null && visited.add(terminal)) {
        terminal = (terminal.resolveRef(spec) ?: break).effectiveSchema()
    }
    when (terminal.getSpecType()) {
        is SpecSchemaType.Primitive,
        SpecSchemaType.Enum,
        SpecSchemaType.Array,
        SpecSchemaType.Map -> throw IllegalStateException(
            "cannot generate '${name()}': the schema combines a nullable oneOf/anyOf wrapper with sibling keywords, " +
                    "but the remaining variant is not an object and cannot be merged into one"
        )

        else -> Unit
    }
}

/**
 * Collects the effective `required` property names, following the same structure as
 * [resolveProperties]: `$ref` targets, `allOf` members and the single non-null variant of
 * a nullable wrapper. General `anyOf` members are excluded: only one of them has to match,
 * so their `required` lists must not constrain the merged object.
 */
fun Schema<*>.resolveRequired(spec: OpenAPI): Set<String> {
    val collected = mutableSetOf<String>()
    val visited: MutableSet<Schema<*>> = Collections.newSetFromMap(IdentityHashMap())

    fun Schema<*>.inner() {
        if (!visited.add(this)) return
        resolveRef(spec)?.let {
            it.inner()
            return
        }
        singleNonNullVariant()?.inner()
        allOf?.forEach { it.inner() }
        required?.let { collected += it }
    }

    inner()
    return collected
}

fun Schema<*>.fullDescription() = listOfNotNull(
    description,
    example?.let { "Example: $it" },
    examples?.joinToString("\n\t", prefix = "Examples:\n\t") { it.toString() }
).joinToString("\n").ifBlank { null }

fun String.refAsTypeNames() =
    removePrefix("#/components/schemas/").split('/').map { it.asTypeName() }

fun Schema<*>.resolveRef(
    spec: OpenAPI
): Schema<*>? {
    val name = `$ref`?.substringAfterLast('/') ?: return null
    val ref = spec.components?.schemas?.get(name)
    require(ref != null) { "cannot resolve schema ref '$`$ref`'" }
    return ref
}

fun Parameter.resolveRef(
    spec: OpenAPI
): Parameter? {
    val name = `$ref`?.substringAfterLast('/') ?: return null
    val ref = spec.components?.parameters?.get(name)
    require(ref != null) { "cannot resolve parameter ref '$`$ref`'" }
    return ref
}

fun RequestBody.resolveRef(
    spec: OpenAPI
): RequestBody? {
    val name = `$ref`?.substringAfterLast('/') ?: return null
    val ref = spec.components?.requestBodies?.get(name)
    require(ref != null) { "cannot resolve requestBody ref '$`$ref`'" }
    return ref
}

fun ApiResponse.resolveRef(
    spec: OpenAPI
): ApiResponse? {
    val name = `$ref`?.substringAfterLast('/') ?: return null
    val ref = spec.components?.responses?.get(name)
    require(ref != null) { "cannot resolve response ref '$`$ref`'" }
    return ref
}

fun Schema<*>.resolveProperties(
    spec: OpenAPI,
    ignoreProperties: Set<String>
): Map<String, Schema<*>> {
    val collectedProperties: MutableMap<String, Schema<*>> = mutableMapOf()
    val visitedSchemas: MutableSet<Int> = mutableSetOf()

    fun Schema<*>.inner(
        ignoreProperties: Set<String>
    ) {
        val hash = System.identityHashCode(this)
        if (hash in visitedSchemas) return
        visitedSchemas += hash

        val localIgnoreProperties = discriminator?.propertyName?.let { discriminator ->
            ignoreProperties + discriminator
        } ?: ignoreProperties

        resolveRef(spec)?.let {
            it.inner(localIgnoreProperties)
            return
        }
        singleNonNullVariant()?.inner(localIgnoreProperties)
        anyOf?.forEach { child ->
            child.inner(localIgnoreProperties)
        }
        allOf?.forEach { child ->
            child.inner(localIgnoreProperties)
        }

        properties?.filter { (propertyName, _) ->
            propertyName !in localIgnoreProperties
        }?.forEach { (propertyName, propertySchema) ->
            // a redeclaration without type information only annotates the inherited property
            if (propertyName !in collectedProperties || propertySchema.definesType()) {
                collectedProperties[propertyName] = propertySchema
            }
        }
    }

    inner(ignoreProperties)

    return collectedProperties
}

private fun Schema<*>.definesType(): Boolean =
    `$ref` != null ||
            type != null ||
            !types.isNullOrEmpty() ||
            !enum.isNullOrEmpty() ||
            items != null ||
            !properties.isNullOrEmpty() ||
            additionalProperties != null ||
            oneOf != null ||
            anyOf != null ||
            allOf != null

private fun Schema<*>.resolveType(): String {
    if (type != null) return type
    types?.forEach {
        if (it in allTypes) {
            return it
        }
    }
    return "object"
}

private val primitiveTypes = setOf(
    "string",
    "integer",
    "number",
    "boolean"
)

private val allTypes = setOf("object", "array") + primitiveTypes

fun Operation.resolveTags() = tags.orEmpty().ifEmpty { listOf(Constants.FALLBACK_TAG) }

private val pathParamRegex = """[{](\S+?)[}]""".toRegex()

fun Server.asBaseUrl(): String {
    val resolvedUrl = url.orEmpty().replace(pathParamRegex) { r ->
        val key = r.groupValues[1]
        variables?.get(key)?.run {
            default ?: enum?.firstOrNull()
        } ?: r.value
    }
    return resolvedUrl.asBaseUrl()
}
