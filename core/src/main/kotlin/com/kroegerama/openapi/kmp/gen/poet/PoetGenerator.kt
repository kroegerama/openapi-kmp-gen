package com.kroegerama.openapi.kmp.gen.poet

import com.kroegerama.openapi.kmp.gen.OptionSet
import com.kroegerama.openapi.kmp.gen.spec.SpecApi
import com.kroegerama.openapi.kmp.gen.spec.SpecModel
import com.kroegerama.openapi.kmp.gen.spec.SpecOperation
import com.kroegerama.openapi.kmp.gen.spec.SpecParameter
import com.kroegerama.openapi.kmp.gen.spec.SpecPrimitiveType
import com.kroegerama.openapi.kmp.gen.spec.SpecProperty
import com.kroegerama.openapi.kmp.gen.spec.SpecSchema
import com.kroegerama.openapi.kmp.gen.spec.SpecSecurityScheme
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MAP
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.buildCodeBlock
import com.squareup.kotlinpoet.withIndent
import java.time.format.DateTimeFormatter

class PoetGenerator(
    private val specModel: SpecModel,
    private val options: OptionSet
) {
    private val types = PoetTypes(options)

    fun createFileSpecs(): List<FileSpec> {
        val apiFile = createApiFile()
        val authFile = createAuthFile()
        val modelFile = createModelFile()
        val servicesFile = createServicesFile()

        return listOf(
            apiFile,
            authFile,
            modelFile,
            servicesFile
        )
    }

    private fun createApiFile(): FileSpec {
        return poetFile(
            packageName = options.packageName,
            fileName = types.api.simpleName
        ) {
            addFileComment("%L", specModel.fileHeader)
            addType(createApi())
        }
    }

    private fun createAuthFile(): FileSpec {
        return poetFile(
            packageName = options.packageName,
            fileName = types.auth.simpleName
        ) {
            addFileComment("%L", specModel.fileHeader)
            addType(createAuth())
        }
    }

    private fun createModelFile(): FileSpec {
        return poetFile(
            packageName = options.modelPackage,
            fileName = "Models"
        ) {
            addFileComment("%L", specModel.fileHeader)

            addAnnotation(
                poetAnnotation(Suppress::class.asTypeName()) {
                    addMember("%S", "ArrayInDataClass")
                }
            )

            val (types, typeAliases) = createTypes()
            addTypes(types)
            typeAliases.forEach(::addTypeAlias)
        }
    }

    private fun createServicesFile(): FileSpec {
        return poetFile(
            packageName = options.apiPackage,
            fileName = "Services"
        ) {
            addFileComment("%L", specModel.fileHeader)
            addTypes(createServices())
        }
    }

    private fun createApi(): TypeSpec {
        return poetObject(types.api) {
            superclass(PoetTypes.ApiHolder)

            val title = poetProperty("title", STRING, KModifier.CONST) {
                initializer("%S", specModel.metadata.title)
            }
            val description = poetProperty("description", STRING, KModifier.CONST) {
                initializer("%S", specModel.metadata.description.orEmpty())
            }
            val version = poetProperty("version", STRING, KModifier.CONST) {
                initializer("%S", specModel.metadata.version)
            }
            val createdAt = poetProperty("createdAt", STRING, KModifier.CONST) {
                initializer(
                    "%S",
                    specModel.metadata.createdAt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                )
            }
            val servers = poetProperty("servers", PoetTypes.ListOfKtorUrl) {
                initializer(
                    buildCodeBlock {
                        addStatement("%M(", MemberName("kotlin.collections", "listOf"))
                        withIndent {
                            specModel.metadata.servers.forEach { server ->
                                addStatement("%T(%S),", PoetTypes.KtorUrl, server)
                            }
                        }
                        addStatement(")")
                    }
                )
            }
            val baseUrl = poetProperty("baseUrl", PoetTypes.KtorUrl) {
                addModifiers(KModifier.OVERRIDE)
                mutable(true)
                initializer("servers.first()")
            }
            val setAuthProvider = poetFunSpec("setAuthProvider") {
                val auth = poetParameter("auth", types.auth) { }
                addParameter(auth)
                addStatement("setAuthProvider(auth.key, auth::provideAuthItem)")
            }
            val clearAuthProvider = poetFunSpec("clearAuthProvider") {
                val auth = poetParameter("auth", types.auth) { }
                addParameter(auth)
                addStatement("clearAuthProvider(auth.key)")
            }

            addProperties(
                listOf(
                    title,
                    description,
                    version,
                    createdAt,
                    servers,
                    baseUrl
                )
            )
            addFunctions(
                listOf(
                    setAuthProvider,
                    clearAuthProvider
                )
            )
        }
    }

    private fun createAuth(): TypeSpec {
        return poetInterface(types.auth) {
            addModifiers(KModifier.SEALED)
            val key = poetProperty("key", STRING) { }
            val provideAuthItem = poetFunSpec("provideAuthItem") {
                addModifiers(KModifier.ABSTRACT, KModifier.SUSPEND)
                returns(PoetTypes.AuthItem.nullable())
            }
            addProperty(key)
            addFunction(provideAuthItem)

            addTypes(createAuthItems())
        }
    }

    private fun createServices(): List<TypeSpec> {
        return specModel.apis.map { api ->
            createService(api)
        }
    }

    private fun createService(api: SpecApi): TypeSpec {
        return poetObject(types.apiName(api.name)) {
            addFunctions(api.operations.map(::createOperation))
        }
    }

    private fun createOperation(operation: SpecOperation): FunSpec {
        return poetFunSpec(operation.name) {
            addModifiers(KModifier.SUSPEND)

            val kdoc = listOfNotNull(
                operation.summary?.let { "**$it**" },
                "`${operation.method} ${operation.path}`",
                operation.description
            ).joinToString("\n\n")
            addKdoc(kdoc)

            val response = convertSimpleType(operation.response.type)
            val either = PoetTypes.either(response)
            returns(
                returnType = either,
                kdoc = operation.response.description?.let { CodeBlock.of("%L", it) } ?: CodeBlock.builder().build()
            )
            if (operation.deprecated) {
                addAnnotation(deprecated())
            }
            addParameters(operation.parameters.map(::createParameter))

            operation.body?.let { body ->
                addParameter(
                    when (operation.type) {
                        SpecOperation.Type.Default -> createBodyParameter(body)
                        SpecOperation.Type.Multipart -> createMultipartBodyParameter(body)
                        SpecOperation.Type.UrlEncoded -> createUrlEncodedBodyParameter(body)
                        SpecOperation.Type.Unknown -> createAnyBodyParameter(body)
                    }
                )
            }

            val decoratorType = LambdaTypeName.get(
                receiver = PoetTypes.HttpRequestBuilder,
                returnType = UNIT
            )
            addParameter(
                poetParameter(
                    "decorator",
                    decoratorType
                ) {
                    defaultValue("{}")
                }
            )

            beginControlFlow("return %T.client.%M", types.api, PoetMembers.EitherRequest)

            addStatement("method = %T.parse(%S)", PoetTypes.HttpMethod, operation.method.name)

            if (operation.body != null) {
                when (operation.type) {
                    SpecOperation.Type.Default -> addStatement("%M(%T)", PoetMembers.ContentType, PoetTypes.ContentTypeApplicationJson)
                    SpecOperation.Type.Multipart -> Unit // content type is added automatically by ktor
                    SpecOperation.Type.UrlEncoded -> Unit // content type is added automatically by ktor
                    SpecOperation.Type.Unknown -> Unit
                }
            }

            if (operation.securityIds.isNotEmpty()) {
                val authKeysCodeBlock = buildCodeBlock {
                    addStatement("%M(", PoetMembers.AuthKeys)
                    withIndent {
                        operation.securityIds.forEach { securityId ->
                            addStatement("%T.ID,", types.auth.nestedClass(securityId))
                        }
                    }
                    addStatement(")")
                }
                addCode(authKeysCodeBlock)
            }

            operation.serverOverride?.let { override ->
                addStatement("url.%M(%S)", PoetMembers.TakeFrom, override)
            }

            val pathCodeBlock = buildCodeBlock {
                addStatement("url.%M(", PoetMembers.AppendPathSegments)
                withIndent {
                    operation.path.trimStart('/').split('/').forEach { part ->
                        if (part.startsWith('{') && part.endsWith('}')) {
                            val parameter = operation.parameters.first {
                                it.type == SpecParameter.Type.Path && "{${it.rawName}}" == part
                            }
                            addStatement(
                                "%M(value = %L, explode = %L, json = %T.json),",
                                PoetMembers.CreateSerializedPathSegment,
                                parameter.name,
                                parameter.explode,
                                types.api
                            )
                        } else {
                            addStatement("%S,", part)
                        }
                    }
                }
                addStatement(")")
            }
            addCode(pathCodeBlock)

            operation.parameters.forEach { parameter ->
                val appendParameter = when (parameter.type) {
                    SpecParameter.Type.Path -> return@forEach
                    SpecParameter.Type.Cookie -> PoetMembers.AppendSerializedCookieParameter
                    SpecParameter.Type.Header -> PoetMembers.AppendSerializedHeaderParameter
                    SpecParameter.Type.Query -> PoetMembers.AppendSerializedQueryParameter
                }
                addStatement(
                    "%M(name = %S, value = %L, explode = %L, json = %T.json)",
                    appendParameter,
                    parameter.rawName,
                    parameter.name,
                    parameter.explode,
                    types.api
                )
            }

            operation.body?.let {
                addStatement("%M(body)", PoetMembers.RequestSetBody)
            }

            addStatement("decorator()")
            endControlFlow()
        }
    }

    private fun createParameter(parameter: SpecParameter): ParameterSpec {
        return poetParameter(parameter.name, convertSimpleType(parameter.schema).nullable(parameter.nullable)) {
            if (parameter.nullable) {
                defaultValue("null")
            }
            parameter.description?.let {
                addKdoc("%L", it)
            }
        }
    }

    private fun createMultipartBodyParameter(info: SpecOperation.SchemaInfo): ParameterSpec {
        return poetParameter("body", PoetTypes.MultiPartFormDataContent.nullable(info.nullable)) {
            if (info.nullable) {
                defaultValue("null")
            }
            info.description?.let {
                addKdoc("%L", it)
            }
        }
    }

    private fun createUrlEncodedBodyParameter(info: SpecOperation.SchemaInfo): ParameterSpec {
        return poetParameter("body", PoetTypes.FormDataContent.nullable(info.nullable)) {
            if (info.nullable) {
                defaultValue("null")
            }
            info.description?.let {
                addKdoc("%L", it)
            }
        }
    }

    private fun createAnyBodyParameter(info: SpecOperation.SchemaInfo): ParameterSpec {
        return poetParameter("body", ANY.nullable(info.nullable)) {
            if (info.nullable) {
                defaultValue("null")
            }
            info.description?.let {
                addKdoc("%L", it)
            }
        }
    }

    private fun createBodyParameter(info: SpecOperation.SchemaInfo): ParameterSpec {
        return poetParameter("body", convertSimpleType(info.type).nullable(info.nullable)) {
            if (info.nullable) {
                defaultValue("null")
            }
            info.description?.let {
                addKdoc("%L", it)
            }
        }
    }

    private fun createAuthItems(): List<TypeSpec> {
        return specModel.securitySchemes.map { scheme ->
            val className = types.auth.nestedClass(scheme.name)
            poetClass(className) {
                addModifiers(KModifier.DATA)
                addSuperinterface(types.auth)
                when (scheme.type) {
                    SpecSecurityScheme.Type.Basic -> {
                        val getBasic = poetProperty(
                            "getBasic",
                            LambdaTypeName.get(
                                returnType = PoetTypes.AuthItemBasic.nullable()
                            ).copy(
                                suspending = true
                            )
                        ) {}
                        val key = poetProperty("key", STRING) {
                            addModifiers(KModifier.OVERRIDE)
                            initializer("ID")
                        }
                        val provideAuthItem = poetFunSpec("provideAuthItem") {
                            addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                            returns(PoetTypes.AuthItem.nullable())
                            addStatement("return getBasic()")
                        }
                        primaryConstructor(getBasic)
                        addProperty(key)
                        addFunction(provideAuthItem)
                    }

                    SpecSecurityScheme.Type.Bearer -> {
                        val getBearer = poetProperty(
                            "getBearer",
                            LambdaTypeName.get(
                                returnType = PoetTypes.AuthItemBearer.nullable()
                            ).copy(
                                suspending = true
                            )
                        ) {}
                        val key = poetProperty("key", STRING) {
                            addModifiers(KModifier.OVERRIDE)
                            initializer("ID")
                        }
                        val provideAuthItem = poetFunSpec("provideAuthItem") {
                            addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                            returns(PoetTypes.AuthItem.nullable())
                            addStatement("return getBearer()")
                        }
                        primaryConstructor(getBearer)
                        addProperty(key)
                        addFunction(provideAuthItem)
                    }

                    SpecSecurityScheme.Type.Header,
                    SpecSecurityScheme.Type.Query,
                    SpecSecurityScheme.Type.Cookie -> {
                        val typePart = scheme.type.name
                        val getValueName = "get${typePart}Value"

                        val getValue = poetProperty(
                            getValueName,
                            LambdaTypeName.get(
                                returnType = STRING.nullable()
                            ).copy(
                                suspending = true
                            )
                        ) {}
                        val key = poetProperty("key", STRING) {
                            addModifiers(KModifier.OVERRIDE)
                            initializer("ID")
                        }
                        val provideAuthItem = poetFunSpec("provideAuthItem") {
                            addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                            returns(PoetTypes.AuthItem.nullable())
                            addCode(
                                buildCodeBlock {
                                    beginControlFlow("return $getValueName()?.let {")
                                    addStatement("%T(", PoetTypes.AuthItemApiKey)
                                    withIndent {
                                        addStatement("position = %T.$typePart,", PoetTypes.AuthItemPosition)
                                        addStatement("name = %S,", scheme.propertyName)
                                        addStatement("value = it")
                                    }
                                    addStatement(")")
                                    endControlFlow()
                                }
                            )
                        }
                        primaryConstructor(getValue)
                        addProperty(key)
                        addFunction(provideAuthItem)
                    }
                }
                val companion = poetCompanionObject {
                    val id = poetProperty("ID", STRING) {
                        addModifiers(KModifier.CONST)
                        initializer("%S", scheme.rawName)
                    }
                    addProperty(id)
                }
                addType(companion)
            }
        }
    }

    private fun createTypes(): Pair<List<TypeSpec>, List<TypeAliasSpec>> {
        val typeAliases = mutableListOf<TypeAliasSpec>()

        fun inner(schemas: List<SpecSchema.NamedSpecSchema>): List<TypeSpec> {
            return schemas.mapNotNull { schema ->
                val name = types.modelName(*schema.typeNames.toTypedArray())
                when (schema) {
                    is SpecSchema.Enum -> poetEnum(name) {
                        addAnnotation(serializable())
                        if (schema.deprecated) {
                            addAnnotation(deprecated())
                        }
                        schema.description?.let {
                            addKdoc("%L", it)
                        }

                        addAnnotation(immutable())
                        schema.constants.forEach { constant ->
                            addEnumConstant(
                                name = constant.name,
                                typeSpec = poetAnonymousClass {
                                    addAnnotation(serialName(constant.value))
                                }
                            )
                        }
                    }

                    is SpecSchema.Object -> poetClass(name) {
                        addModifiers(KModifier.DATA)
                        addAnnotation(serializable())
                        if (schema.deprecated) {
                            addAnnotation(deprecated())
                        }
                        schema.description?.let {
                            addKdoc("%L", it)
                        }

                        addAnnotation(immutable())
                        specModel.modelSerialNames[schema.typeNames]?.let { serialName ->
                            addAnnotation(serialName(serialName))
                        }
                        val properties: Array<PropertySpec> = schema.properties.map(::convertSpecProperty).toTypedArray()
                        primaryConstructor(*properties)
                        addTypes(inner(schema.children))

                        specModel.modelInterfaces[schema.typeNames]?.let { interfaces ->
                            interfaces.forEach {
                                addSuperinterface(types.modelName(*it.toTypedArray()))
                            }
                        }
                    }

                    is SpecSchema.Sealed -> poetInterface(name) {
                        addModifiers(KModifier.SEALED)
                        addAnnotation(serializable())
                        if (schema.deprecated) {
                            addAnnotation(deprecated())
                        }
                        schema.description?.let {
                            addKdoc("%L", it)
                        }

                        addAnnotation(immutable())
                        addAnnotation(discriminator(schema.discriminator))
                        addTypes(inner(schema.children))
                    }

                    is SpecSchema.Typealias -> {
                        typeAliases += poetTypeAlias(
                            name = schema.typeNames.joinToString(""),
                            typeName = convertSimpleType(schema.schema)
                        ) {
                            if (schema.deprecated) {
                                addAnnotation(deprecated())
                            }
                            schema.description?.let {
                                addKdoc("%L", it)
                            }
                        }

                        null
                    }
                }
            }
        }

        val types = inner(specModel.schemas)
        return types to typeAliases
    }

    private fun convertSpecProperty(property: SpecProperty): PropertySpec {
        val typeName = convertSimpleType(property.type).nullable(
            nullable = property.nullable
        )
        return poetProperty(property.name, typeName) {
            addAnnotation(serialName(property.rawName))
            if (property.deprecated) {
                addAnnotation(deprecated())
            }
            property.description?.let {
                addKdoc("%L", it)
            }
        }
    }

    private fun convertSimpleType(simpleType: SpecSchema.SimpleType): TypeName = when (simpleType) {
        is SpecSchema.Primitive -> when (simpleType.type) {
            SpecPrimitiveType.Boolean -> BOOLEAN
            SpecPrimitiveType.Int32 -> INT
            SpecPrimitiveType.Int64 -> LONG
            SpecPrimitiveType.Float -> FLOAT
            SpecPrimitiveType.Double -> DOUBLE
            SpecPrimitiveType.String -> STRING
            SpecPrimitiveType.Date -> PoetTypes.LocalDate
            SpecPrimitiveType.Time -> PoetTypes.LocalTime
            SpecPrimitiveType.DateTime -> PoetTypes.Instant
            SpecPrimitiveType.Base64 -> PoetTypes.SerializableBase64
            SpecPrimitiveType.EpochSeconds -> PoetTypes.SerializableEpochSeconds
            SpecPrimitiveType.EpochMilliseconds -> PoetTypes.SerializableEpochMilliseconds
            SpecPrimitiveType.UUID -> PoetTypes.Uuid
        }

        is SpecSchema.Array -> LIST.parameterizedBy(
            convertSimpleType(simpleType.items).nullable(simpleType.itemsNullable)
        )

        is SpecSchema.Map -> MAP.parameterizedBy(
            STRING,
            convertSimpleType(simpleType.items).nullable(simpleType.itemsNullable)
        )

        is SpecSchema.Ref -> types.modelName(*simpleType.typeNames.toTypedArray())
        is SpecSchema.AnyComplex -> PoetTypes.JsonElement
        SpecSchema.Raw -> PoetTypes.HttpResponse
        SpecSchema.Unit -> UNIT
    }

    private fun serializable() = poetAnnotation(PoetTypes.Serializable) {}

    private fun discriminator(discriminator: String) = poetAnnotation(PoetTypes.JsonClassDiscriminator) {
        addMember("%S", discriminator)
    }

    private fun serialName(name: String) = poetAnnotation(PoetTypes.SerialName) {
        addMember("%S", name)
    }

    private fun deprecated() = poetAnnotation(PoetTypes.Deprecated) {
        addMember("%S", "Deprecated via OpenAPI Spec")
    }

    private fun immutable() = poetAnnotation(PoetTypes.Immutable) {}

}
