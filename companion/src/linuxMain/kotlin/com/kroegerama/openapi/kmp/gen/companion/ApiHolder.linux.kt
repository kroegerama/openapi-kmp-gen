package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.curl.Curl
import io.ktor.utils.io.KtorDsl
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.uname
import platform.posix.utsname

@OptIn(ExperimentalForeignApi::class)
public actual val platformUserAgent: String = memScoped {
    val uts = alloc<utsname>()
    uname(uts.ptr)
    val sysName = uts.sysname.toKString()
    val release = uts.release.toKString()
    "curl $sysName/$release"
}

@KtorDsl
public actual fun createPlatformHttpClient(decorator: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(Curl) {
        decorator()
    }
}
