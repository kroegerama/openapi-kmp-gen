@file:Suppress("ArrayInDataClass")

package generated.models

import com.kroegerama.openapi.kmp.gen.companion.SerializableBase64
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class Photo(
    @SerialName("albumId")
    val albumId: Int,
    @SerialName("id")
    val id: Int,
    @SerialName("title")
    val title: String,
    @SerialName("url")
    val url: String,
    @SerialName("thumbnailUrl")
    val thumbnailUrl: String
)

@Serializable
data class SerialTest(
    val date: LocalDate,
    val time: LocalTime,
    val instant: Instant,
    val uuid: Uuid,
    val base64: SerializableBase64
)
