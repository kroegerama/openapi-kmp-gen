package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.curl.Curl
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.uname
import platform.posix.utsname

@OptIn(ExperimentalForeignApi::class)
internal actual val platformUserAgent: String = memScoped {
    val uts = alloc<utsname>()
    uname(uts.ptr)
    val sysName = uts.sysname.toKString()
    val release = uts.release.toKString()
    "curl $sysName/$release"
}

internal actual fun createBaseClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(Curl) {
        block()
    }
}
