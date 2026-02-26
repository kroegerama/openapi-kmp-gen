package com.kroegerama.openapi.kmp.gen.poet

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import kotlin.reflect.KClass

fun poetFile(packageName: String, fileName: String, block: FileSpec.Builder.() -> Unit) =
    FileSpec.builder(packageName, fileName).apply(block).build()

fun poetTypeAlias(name: String, typeName: TypeName, block: TypeAliasSpec.Builder.() -> Unit) =
    TypeAliasSpec.builder(name, typeName).apply(block).build()

fun poetClass(className: ClassName, block: TypeSpec.Builder.() -> Unit) =
    TypeSpec.classBuilder(className).apply(block).build()

fun poetObject(objName: ClassName, block: TypeSpec.Builder.() -> Unit) =
    TypeSpec.objectBuilder(objName).apply(block).build()

fun poetEnum(className: ClassName, block: TypeSpec.Builder.() -> Unit) =
    TypeSpec.enumBuilder(className).apply(block).build()

fun poetAnonymousClass(block: TypeSpec.Builder.() -> Unit) =
    TypeSpec.anonymousClassBuilder().apply(block).build()

fun poetFunSpec(name: String, block: FunSpec.Builder.() -> Unit) =
    FunSpec.builder(name).apply(block).build()

fun poetConstructor(block: FunSpec.Builder.() -> Unit) =
    FunSpec.constructorBuilder().apply(block).build()

fun poetAnnotation(className: ClassName, block: AnnotationSpec.Builder.() -> Unit) =
    AnnotationSpec.builder(className).apply(block).build()

fun poetCompanionObject(name: String? = null, block: TypeSpec.Builder.() -> Unit) =
    TypeSpec.companionObjectBuilder(name).apply(block).build()

fun poetParameter(
    name: String,
    typeName: TypeName,
    vararg modifiers: KModifier,
    block: ParameterSpec.Builder.() -> Unit
) = ParameterSpec.builder(name, typeName, *modifiers).apply(block).build()

fun poetProperty(
    name: String,
    typeName: TypeName,
    vararg modifiers: KModifier,
    block: PropertySpec.Builder.() -> Unit
) = PropertySpec.builder(name, typeName, *modifiers).apply(block).build()

fun poetProperty(
    name: String,
    kcls: KClass<*>,
    vararg modifiers: KModifier,
    block: PropertySpec.Builder.() -> Unit
) = PropertySpec.builder(name, kcls, *modifiers).apply(block).build()

fun TypeName.nullable(nullable: Boolean = true) =
    if (this.isNullable == nullable) this else copy(nullable)

fun TypeName.notNull() = nullable(false)

fun TypeSpec.Builder.primaryConstructor(
    vararg properties: PropertySpec,
    decorateParameter: ParameterSpec.Builder.(Int) -> Unit
): TypeSpec.Builder {
    val propertySpecs = properties.map { p -> p.toBuilder().initializer(p.name).build() }
    val parameters = propertySpecs.mapIndexed { index, spec ->
        ParameterSpec.builder(spec.name, spec.type).apply {
            decorateParameter(index)
        }.build()
    }
    val constructor = FunSpec.constructorBuilder()
        .addParameters(parameters)
        .build()

    return this
        .primaryConstructor(constructor)
        .addProperties(propertySpecs)
}

fun poetInterface(className: ClassName, block: TypeSpec.Builder.() -> Unit) =
    TypeSpec.interfaceBuilder(className).apply(block).build()