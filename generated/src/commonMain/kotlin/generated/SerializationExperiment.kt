package generated

import com.kroegerama.openapi.kmp.gen.companion.encodeToPrimitiveString
import generated.models.Photo
import generated.models.SerialTest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.uuid.Uuid

fun main() {
    val json = Api.json

    println(json.encodeToPrimitiveString<TestEnum?>(null))
    println(json.encodeToPrimitiveString(TestEnum.A))
    println(json.encodeToPrimitiveString(TestEnum.B))
    println(json.encodeToPrimitiveString(Clock.System.now()))
    println(json.encodeToPrimitiveString(Photo(1, 2, "Title", "url", "thumbnail")))
    println(json.encodeToPrimitiveString(listOf("A")))

    val j = """
        {"date":"2025-09-20","time":"13:25:29","instant":"2025-09-20T11:25:29Z","uuid":"bc45632f-edfa-4284-883a-a41a408e0ec6","base64":"SGVsbG8gV29ybGQ="}
    """.trimIndent()
    println(json.decodeFromString<SerialTest>(j))

    val localDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val model = SerialTest(
        date = localDateTime.date,
        time = localDateTime.time,
        instant = Clock.System.now(),
        uuid = Uuid.random(),
        base64 = "Hello World".encodeToByteArray()
    )
    println(model)

    val modelJson = json.encodeToString(model)
    println(modelJson)

    val modelDeserialized = json.decodeFromString<SerialTest>(modelJson)
    println(modelDeserialized)
}

@Serializable
enum class TestEnum {
    A,

    @SerialName("TestEnumB")
    B
}
