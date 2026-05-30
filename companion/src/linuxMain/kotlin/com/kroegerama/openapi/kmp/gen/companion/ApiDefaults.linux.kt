package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.curl.Curl
import io.ktor.client.engine.curl.CurlClientEngineConfig
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.uname
import platform.posix.utsname

public actual typealias PlatformHttpClientEngineConfig = CurlClientEngineConfig

@OptIn(ExperimentalForeignApi::class)
public actual val platformUserAgent: String = memScoped {
    val uts = alloc<utsname>()
    uname(uts.ptr)
    val sysName = uts.sysname.toKString()
    val release = uts.release.toKString()
    "curl $sysName/$release"
}

public actual fun createPlatformHttpClient(decorator: HttpClientConfig<PlatformHttpClientEngineConfig>.() -> Unit): HttpClient {
    return HttpClient(Curl) {
        decorator()
    }
}
