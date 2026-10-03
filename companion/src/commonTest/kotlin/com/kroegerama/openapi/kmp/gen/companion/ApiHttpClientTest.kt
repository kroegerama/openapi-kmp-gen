package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ApiHttpClientTest {

    @Test
    fun platformOverloadUsesThePlatformEngine() {
        val platformEngine = PlatformHttpClientEngineFactory.create()
        val client = createApiHttpClient()
        try {
            assertEquals(platformEngine::class, client.engine::class)
        } finally {
            client.close()
            platformEngine.close()
        }
    }

    @Test
    fun platformOverloadInstallsTheLibraryDefaults() {
        val client = createApiHttpClient()
        try {
            assertNotNull(client.pluginOrNull(UserAgent))
            assertNotNull(client.pluginOrNull(ContentNegotiation))
        } finally {
            client.close()
        }
    }

    @Test
    fun platformOverloadSkipsUserAgentWhenNull() {
        val client = createApiHttpClient(userAgent = null)
        try {
            assertNull(client.pluginOrNull(UserAgent))
            assertNotNull(client.pluginOrNull(ContentNegotiation))
        } finally {
            client.close()
        }
    }

    @Test
    fun platformOverloadAppliesTheBlock() {
        val client = createApiHttpClient {
            install(HttpCookies)
        }
        try {
            assertNotNull(client.pluginOrNull(HttpCookies))
        } finally {
            client.close()
        }
    }

    @Test
    fun engineInstanceOverloadUsesTheGivenEngine() = runTest {
        val engine = MockEngine { respond("") }
        val client = createApiHttpClient(engine, userAgent = "test-agent/1.0") {
            install(HttpCookies)
        }
        try {
            assertSame(engine, client.engine)
            assertEquals(HttpStatusCode.OK, client.get("https://api.example.com/x").status)
            assertEquals("test-agent/1.0", engine.requestHistory.single().headers[HttpHeaders.UserAgent])
            assertNotNull(client.pluginOrNull(ContentNegotiation))
            assertNotNull(client.pluginOrNull(HttpCookies))
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun contentNegotiationUsesTheJsonPassedIn() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val handler = recordingJsonHandler(requests, """{"first_name":"Ada"}""")
        val callerEngine = MockEngine(handler)
        val factory = createApiHttpClient(MockEngine, json = snakeCaseJson) {
            engine { addHandler(handler) }
        }
        val instance = createApiHttpClient(callerEngine, json = snakeCaseJson)
        try {
            for (client in listOf(factory, instance)) {
                val result = client.eitherRequest<Person> {
                    method = HttpMethod.Post
                    url("https://api.example.com/x")
                    contentType(ContentType.Application.Json)
                    setBody(Person("Ada"))
                }
                assertEquals(Person("Ada"), result.getOrNull()?.data, result.toString())
            }
            assertEquals(List(2) { """{"first_name":"Ada"}""" }, requests.map { (it.body as TextContent).text })
        } finally {
            factory.close()
            instance.close()
            callerEngine.close()
        }
    }

    @Test
    fun engineInstanceOverloadValidatesSuccess() = runTest {
        val engine = MockEngine { respond("nope", HttpStatusCode.NotFound) }
        val client = createApiHttpClient(engine)
        try {
            assertFailsWith<ClientRequestException> {
                client.get("https://api.example.com/x")
            }
        } finally {
            client.close()
            engine.close()
        }
    }

    @Test
    fun closingTheClientLeavesTheEngineInstanceUsable() = runTest {
        val engine = MockEngine { respond("") }
        val closed = createApiHttpClient(engine)
        closed.close()
        val client = createApiHttpClient(engine)
        try {
            assertFalse(closed.isActive)
            assertTrue(engine.isActive)
            assertEquals(HttpStatusCode.OK, client.get("https://api.example.com/x").status)
        } finally {
            client.close()
            engine.close()
        }
    }
}
