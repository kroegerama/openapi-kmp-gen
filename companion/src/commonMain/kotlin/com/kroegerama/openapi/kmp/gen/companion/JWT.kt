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
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.io.encoding.Base64
import kotlin.time.Clock
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
    public val expiresAt: Instant? = payload.exp?.let { Instant.fromEpochSeconds(it) }
    public val notBefore: Instant? = payload.nbf?.let { Instant.fromEpochSeconds(it) }
    public val issuedAt: Instant? = payload.iat?.let { Instant.fromEpochSeconds(it) }
    public val id: String? = payload.jti

    public fun getClaim(name: String): JsonElement? = claims[name]

    /**
     * checks only exp (expiresAt)
     */
    public fun isExpired(leeway: Long = 0L): Boolean {
        require(leeway >= 0) { "The leeway must be a positive value. Got $leeway instead." }

        val now = Clock.System.now().epochSeconds
        val exp = expiresAt?.epochSeconds

        return exp != null && now > exp + leeway
    }

    /**
     * checks all time related fields: exp (expiresAt), nbf (notBefore), and iat (issuedAt)
     */
    public fun isTimeValid(leeway: Long = 0L): Boolean {
        require(leeway >= 0) { "The leeway must be a positive value. Got $leeway instead." }

        val now = Clock.System.now().epochSeconds
        val exp = expiresAt?.epochSeconds
        val nbf = notBefore?.epochSeconds
        val iat = issuedAt?.epochSeconds

        if (exp != null && now > exp + leeway) return false
        if (nbf != null && now < nbf - leeway) return false
        if (iat != null && iat > now + leeway) return false

        return true
    }

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

@Serializable
public data class JWTPayload(
    @SerialName("iss")
    val iss: String? = null,
    @SerialName("sub")
    val sub: String? = null,
    @SerialName("aud")
    @Serializable(with = AudienceSerializer::class)
    val aud: List<String>? = null,
    @SerialName("exp")
    val exp: Long? = null,
    @SerialName("nbf")
    val nbf: Long? = null,
    @SerialName("iat")
    val iat: Long? = null,
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
        throw UnsupportedOperationException()
    }
}
