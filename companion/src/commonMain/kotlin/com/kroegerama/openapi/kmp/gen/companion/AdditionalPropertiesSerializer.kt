package com.kroegerama.openapi.kmp.gen.companion

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTransformingSerializer

/**
 * Serializer for a class with a bucket property that receives every JSON key the class does not
 * declare. Decoding moves unknown keys into the bucket; encoding writes them back at the top level
 * and omits the bucket key itself.
 *
 * @param tSerializer the plugin-generated serializer of the class
 * @param bucketName the serial name of the bucket property
 * @param ignoredKeys keys dropped on decoding and rejected as bucket entries on encoding, such as the discriminator of an enclosing sealed type
 * @throws IllegalArgumentException on construction when [tSerializer] declares no property named [bucketName] or that property is not a map
 * @throws SerializationException on encoding when the bucket contains a declared serial name, a `@JsonNames` alias or an ignored key
 */
public open class AdditionalPropertiesSerializer<T : Any>(
    tSerializer: KSerializer<T>,
    private val bucketName: String,
    private val ignoredKeys: Set<String> = emptySet()
) : JsonTransformingSerializer<T>(tSerializer) {

    final override val descriptor: SerialDescriptor get() = super.descriptor

    init {
        val bucketIndex = descriptor.getElementIndex(bucketName)
        require(bucketIndex != CompositeDecoder.UNKNOWN_NAME) {
            "${descriptor.serialName} has no property '$bucketName'"
        }
        val bucketKind = descriptor.getElementDescriptor(bucketIndex).kind
        require(bucketKind == StructureKind.MAP) {
            "${descriptor.serialName}.$bucketName must be a map, but has kind $bucketKind"
        }
    }

    private val declaredKeys: Set<String> = buildSet {
        for (index in 0 until descriptor.elementsCount) {
            val name = descriptor.getElementName(index)
            if (name == bucketName) continue
            add(name)
            descriptor.getElementAnnotations(index)
                .filterIsInstance<JsonNames>()
                .forEach { addAll(it.names.asList()) }
        }
    }

    private val reservedKeys: Set<String> = declaredKeys + ignoredKeys

    override fun transformDeserialize(element: JsonElement): JsonElement {
        val obj = element as? JsonObject ?: return element
        val declared = obj.filterKeys { it in declaredKeys && it !in ignoredKeys }
        val extra = obj.filterKeys { it !in reservedKeys }
        return JsonObject(declared + (bucketName to JsonObject(extra)))
    }

    override fun transformSerialize(element: JsonElement): JsonElement {
        val obj = element as? JsonObject ?: return element
        val bucket = obj[bucketName] as? JsonObject ?: return JsonObject(obj - bucketName)
        bucket.keys.firstOrNull { it in reservedKeys }?.let { key ->
            val reason = if (key in declaredKeys) "a declared property of ${descriptor.serialName}" else "an ignored key of ${descriptor.serialName}"
            throw SerializationException("additional property '$key' collides with $reason")
        }
        return JsonObject(obj - bucketName + bucket)
    }
}
