package com.kroegerama.openapi.kmp.gen.companion

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.serializers.FormattedInstantSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.io.encoding.Base64
import kotlin.time.Instant

public typealias SerializableBase64 = @Serializable(Base64Serializer::class) ByteArray

public object Base64Serializer : KSerializer<ByteArray> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.kroegerama.openapi.kmp.gen.companion.Base64Serializer", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ByteArray): Unit = encoder.encodeString(Base64.encode(value))
    override fun deserialize(decoder: Decoder): ByteArray = Base64.decode(decoder.decodeString())
}

public typealias SerializableEpochSeconds = @Serializable(EpochSecondsSerializer::class) Instant

public object EpochSecondsSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.kroegerama.openapi.kmp.gen.companion.EpochSecondsSerializer", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant): Unit = encoder.encodeLong(value.epochSeconds)
    override fun deserialize(decoder: Decoder): Instant = Instant.fromEpochSeconds(decoder.decodeLong())
}

public typealias SerializableEpochMilliseconds = @Serializable(EpochMillisecondsSerializer::class) Instant

public object EpochMillisecondsSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.kroegerama.openapi.kmp.gen.companion.EpochMillisecondsSerializer", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Instant): Unit = encoder.encodeLong(value.toEpochMilliseconds())
    override fun deserialize(decoder: Decoder): Instant = Instant.fromEpochMilliseconds(decoder.decodeLong())
}

public typealias SerializableImmutableList<T> = @Serializable(ImmutableListSerializer::class) ImmutableList<T>

public class ImmutableListSerializer<T>(
    elementSerializer: KSerializer<T>
) : KSerializer<ImmutableList<T>> {
    private val listSerializer: KSerializer<List<T>> = ListSerializer(elementSerializer)

    override val descriptor: SerialDescriptor =
        SerialDescriptor("com.kroegerama.openapi.kmp.gen.companion.ImmutableListSerializer", listSerializer.descriptor)

    override fun serialize(encoder: Encoder, value: ImmutableList<T>): Unit = listSerializer.serialize(encoder, value)
    override fun deserialize(decoder: Decoder): ImmutableList<T> = listSerializer.deserialize(decoder).toImmutableList()
}

public typealias SerializableISO8601Instant = @Serializable(ISO8601InstantSerializer::class) Instant

public object ISO8601InstantSerializer : FormattedInstantSerializer(
    "com.kroegerama.openapi.kmp.gen.companion.ISO8601InstantSerializer",
    ISO_8601_FORMAT
)

public val ISO_8601_FORMAT: DateTimeFormat<DateTimeComponents> = DateTimeComponents.Format {
    dateTime(LocalDateTime.Formats.ISO)
    alternativeParsing(
        { offset(UtcOffset.Formats.ISO_BASIC) },
        { offset(UtcOffset.Formats.FOUR_DIGITS) },
    ) { offset(UtcOffset.Formats.ISO) }
}
