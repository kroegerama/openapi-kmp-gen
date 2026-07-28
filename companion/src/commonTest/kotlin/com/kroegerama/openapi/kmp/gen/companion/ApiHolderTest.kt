package com.kroegerama.openapi.kmp.gen.companion

import com.kroegerama.openapi.kmp.gen.companion.AuthPlugin.Plugin.authKeys
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class ApiHolderTest {

    @Serializable
    private data class Dto(val value: Int)

    private class TestApiHolder : ApiHolder() {
        override var baseUrl: Url = Url("https://api.example.com/v1/")

        // expose the protected registration hooks for assertions
        fun register(id: String, provider: AuthItemProvider) = setAuthProvider(id, provider)
        fun unregister(id: String) = clearAuthProvider(id)
    }

    private val requests = mutableListOf<HttpRequestData>()

    /**
     * Injects a [MockEngine] into [ApiHolder.updateClient]. The companion abstracts the engine config
     * behind the [PlatformHttpClientEngineConfig] `expect` type, which is not [MockEngineConfig], so the
     * library's decorator (`defaultConfig` + `apiConfig` + user `decorator`) is applied via an unchecked
     * cast. This is runtime-safe: none of those blocks touch engine-specific configuration.
     */
    private fun mockEngineFactory(
        handler: MockRequestHandler
    ): (HttpClientConfig<PlatformHttpClientEngineConfig>.() -> Unit) -> HttpClient = { decorator ->
        HttpClient(MockEngine) {
            engine {
                addHandler(handler)
            }
            @Suppress("UNCHECKED_CAST")
            (this as HttpClientConfig<PlatformHttpClientEngineConfig>).decorator()
        }
    }

    private val mockFactory = mockEngineFactory { request ->
        requests += request
        respond("")
    }

    private fun holder(json: Json = createDefaultJson()): TestApiHolder =
        TestApiHolder().apply {
            updateClient(json = json, userAgent = null, createHttpClient = mockFactory)
        }

    @Test
    fun jsonReturnsConfiguredInstance() {
        val json = Json { prettyPrint = true }
        assertSame(json, holder(json = json).json)
    }

    @Test
    fun clientIsStableAcrossReads() {
        val holder = holder()
        assertSame(holder.client, holder.client)
    }

    @Test
    fun updateClientClosesPreviousClient() {
        // the replaced client must be closed so its resources are released
        val holder = holder()
        val first = holder.client
        holder.updateClient(userAgent = null, createHttpClient = mockFactory)
        assertFalse(first.isActive, "previous client should be closed")
        assertNotSame(first, holder.client)
    }

    @Test
    fun updateClientReplacesJson() {
        val holder = holder()
        val newJson = Json { prettyPrint = true }
        holder.updateClient(json = newJson, userAgent = null, createHttpClient = mockFactory)
        assertSame(newJson, holder.json)
    }

    @Test
    fun baseUrlIsAppliedToRequests() = runTest {
        // apiConfig's DefaultRequest applies the holder's baseUrl to relative request paths
        val holder = holder()
        holder.client.get("photos/42")
        val request = requests.last()
        assertEquals("api.example.com", request.url.host)
        assertEquals("/v1/photos/42", request.url.encodedPath)
    }

    @Test
    fun registeredAuthProviderIsResolvedPerRequest() = runTest {
        // providers registered after client creation are read live via the atomic map
        val holder = holder()
        holder.register("bearer") { AuthItem.Bearer("tok") }

        holder.client.get("x") { authKeys("bearer") }
        assertEquals("Bearer tok", requests.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun clearedAuthProviderIsRemoved() = runTest {
        val holder = holder()
        holder.register("bearer") { AuthItem.Bearer("tok") }
        holder.unregister("bearer")

        holder.client.get("x") { authKeys("bearer") }
        assertNull(requests.last().headers[HttpHeaders.Authorization])
    }

    @Test
    fun decoratorIsAppliedOnTopOfApiConfig() = runTest {
        val holder = TestApiHolder().apply {
            updateClient(userAgent = null, createHttpClient = mockFactory) {
                defaultRequest {
                    header("X-Decorated", "yes")
                }
            }
        }
        holder.client.get("x")
        val request = requests.last()
        assertEquals("yes", request.headers["X-Decorated"])
        // the decorator's DefaultRequest block must merge with apiConfig's, not replace it
        assertEquals("api.example.com", request.url.host)
    }

    @Test
    fun userAgentIsApplied() = runTest {
        val holder = TestApiHolder().apply {
            updateClient(userAgent = "test-agent/1.0", createHttpClient = mockFactory)
        }
        holder.client.get("x")
        assertEquals("test-agent/1.0", requests.last().headers[HttpHeaders.UserAgent])
    }

    @Test
    fun errorStatusThrowsByDefault() = runTest {
        // defaultConfig sets expectSuccess = true
        val holder = TestApiHolder().apply {
            updateClient(
                userAgent = null,
                createHttpClient = mockEngineFactory {
                    respond("nope", HttpStatusCode.NotFound)
                }
            )
        }
        assertFailsWith<ClientRequestException> {
            holder.client.get("x")
        }
    }

    @Test
    fun contentNegotiationUsesHolderJson() = runTest {
        val factory = mockEngineFactory {
            respond(
                """{"value":1,"extra":true}""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        // createDefaultJson sets ignoreUnknownKeys = true, so the extra field must not fail decoding
        val lenient = TestApiHolder().apply {
            updateClient(userAgent = null, createHttpClient = factory)
        }
        assertEquals(Dto(1), lenient.client.get("x").body())

        // a strict Json must reject the extra field - proving ContentNegotiation is wired to the
        // holder's json instance rather than a default of its own
        val strict = TestApiHolder().apply {
            updateClient(json = Json { ignoreUnknownKeys = false }, userAgent = null, createHttpClient = factory)
        }
        assertFailsWith<ContentConvertException> {
            strict.client.get("x").body<Dto>()
        }
    }
}
