package com.kroegerama.openapi.kmp.gen.companion

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.io.encoding.Base64
import kotlin.math.floor
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * A JWT (JSON Web Token) parser for extracting header, payload, and claims from JWT tokens.
 *
 * **IMPORTANT: This class does NOT validate the token signature.** It is designed solely for
 * parsing JWT tokens and retrieving their claims. Do not use this class for security-critical
 * validation or authentication purposes without implementing proper signature verification.
 *
 * The class provides convenient access to standard JWT claims (issuer, subject, audience, etc.)
 * and allows retrieval of custom claims through the [getClaim] method.
 *
 * @property token The original JWT token string
 * @property header The decoded JWT header as a map of JSON elements
 * @property signature The signature part of the JWT (not validated)
 * @property issuer The "iss" (issuer) claim
 * @property subject The "sub" (subject) claim
 * @property audience The "aud" (audience) claim as a list of strings
 * @property expiresAt The "exp" (expiration time) claim as an Instant
 * @property notBefore The "nbf" (not before) claim as an Instant
 * @property issuedAt The "iat" (issued at) claim as an Instant
 * @property id The "jti" (JWT ID) claim
 */
public class JWT private constructor(
    public val token: String,
    public val header: Map<String, JsonElement>,
    payload: JWTPayload,
    private val claims: Map<String, JsonElement>,
    public val signature: String
) {
    public val issuer: String? = payload.iss
    public val subject: String? = payload.sub
    public val audience: List<String>? = payload.aud
    public val expiresAt: Instant? = payload.exp?.let(::numericDateToInstant)
    public val notBefore: Instant? = payload.nbf?.let(::numericDateToInstant)
    public val issuedAt: Instant? = payload.iat?.let(::numericDateToInstant)
    public val id: String? = payload.jti

    public fun getClaim(name: String): JsonElement? = claims[name]

    /**
     * Checks only exp ([expiresAt]).
     *
     * The leeway is a clock-skew grace period: the token still counts as valid until
     * [leeway] *after* its expiry.
     *
     * @param leeway grace period, added to exp
     * @throws IllegalArgumentException when [leeway] is negative.
     */
    public fun isExpired(leeway: Duration = Duration.ZERO): Boolean {
        require(leeway >= Duration.ZERO) { "The leeway must not be negative. Got $leeway instead." }

        val now = Clock.System.now()
        val exp = expiresAt

        return exp != null && now > exp + leeway
    }

    /**
     * Checks all time related fields: exp ([expiresAt]), nbf ([notBefore]), and iat ([issuedAt]).
     *
     * The leeway is a clock-skew grace period that widens the acceptance window in every
     * direction: exp is extended by [leeway], nbf is accepted up to [leeway] early,
     * and an iat up to [leeway] in the future is tolerated.
     *
     * @param leeway grace period
     * @throws IllegalArgumentException when [leeway] is negative.
     */
    public fun isTimeValid(leeway: Duration = Duration.ZERO): Boolean {
        require(leeway >= Duration.ZERO) { "The leeway must not be negative. Got $leeway instead." }

        val now = Clock.System.now()
        val exp = expiresAt
        val nbf = notBefore
        val iat = issuedAt

        if (exp != null && now > exp + leeway) return false
        if (nbf != null && now < nbf - leeway) return false
        if (iat != null && iat > now + leeway) return false

        return true
    }

    /**
     * Returns the raw [token], so a [JWT] interpolates directly into a header value.
     * Do not log the result — it is the complete, usable token.
     */
    override fun toString(): String = token

    public fun toHumanReadableString(): String {
        return "JWT(issuer=$issuer, subject=$subject, audience=$audience, expiresAt=$expiresAt, notBefore=$notBefore, issuedAt=$issuedAt, id=$id, claims=$claims, header=$header, signature='$signature')"
    }

    public companion object {
        private val Base64JWT = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)
        private val Base64Json = Json {
            ignoreUnknownKeys = true
        }
        private val knownKeys = setOf(
            "iss", "sub", "aud", "exp", "nbf", "iat", "jti"
        )

        private fun splitToken(token: String): List<String> {
            val parts = token.split('.')
            require(parts.size == 3) { "The token must have 3 parts, but has ${parts.size}." }
            return parts
        }

        private fun base64Decode(base64: String): String = Base64JWT.decode(base64).decodeToString()

        public fun parse(token: String): JWT {
            val parts = splitToken(token)
            val header = Base64Json.decodeFromString<Map<String, JsonElement>>(base64Decode(parts[0]))
            val payloadObject = Base64Json.parseToJsonElement(base64Decode(parts[1])).jsonObject
            val signature = parts[2]

            val payload = Base64Json.decodeFromJsonElement<JWTPayload>(payloadObject)
            val claims = payloadObject.filterKeys { it !in knownKeys }

            return JWT(
                token = token,
                header = header,
                payload = payload,
                claims = claims,
                signature = signature
            )
        }

        public fun parseOrNull(token: String): JWT? = try {
            parse(token)
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Converts a JWT `NumericDate` (seconds since epoch, per RFC 7519) to an [Instant].
 *
 * The value is a JSON number that may be non-integer, so any fractional seconds are carried
 * into the nanosecond component. [floor] is used so negative values (pre-epoch) round correctly.
 */
private fun numericDateToInstant(seconds: Double): Instant {
    val wholeSeconds = floor(seconds).toLong()
    val nanoAdjustment = ((seconds - wholeSeconds) * 1_000_000_000).roundToLong()
    return Instant.fromEpochSeconds(wholeSeconds, nanoAdjustment)
}

@Serializable
internal data class JWTPayload(
    @SerialName("iss")
    val iss: String? = null,
    @SerialName("sub")
    val sub: String? = null,
    @SerialName("aud")
    @Serializable(with = AudienceSerializer::class)
    val aud: List<String>? = null,
    @SerialName("exp")
    val exp: Double? = null,
    @SerialName("nbf")
    val nbf: Double? = null,
    @SerialName("iat")
    val iat: Double? = null,
    @SerialName("jti")
    val jti: String? = null
)

internal object AudienceSerializer : KSerializer<List<String>> {

    override val descriptor: SerialDescriptor = ListSerializer(String.serializer()).descriptor

    override fun deserialize(decoder: Decoder): List<String> {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("AudienceSerializer can be used only with JSON")

        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                if (!element.isString) {
                    throw SerializationException("aud must be a string or array of strings")
                }
                listOf(element.content)
            }

            is JsonArray -> {
                element.map {
                    val p = it as? JsonPrimitive
                        ?: throw SerializationException("aud array must contain only strings")
                    if (!p.isString) {
                        throw SerializationException("aud array must contain only strings")
                    }
                    p.content
                }
            }

            else -> throw SerializationException("aud must be a string or array")
        }
    }

    override fun serialize(encoder: Encoder, value: List<String>) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("AudienceSerializer can be used only with JSON")

        // Mirror the deserialize contract: a single audience is written as a bare
        // string, multiple audiences as an array of strings.
        val element = value.singleOrNull()
            ?.let { JsonPrimitive(it) }
            ?: JsonArray(value.map { JsonPrimitive(it) })

        jsonEncoder.encodeJsonElement(element)
    }
}
