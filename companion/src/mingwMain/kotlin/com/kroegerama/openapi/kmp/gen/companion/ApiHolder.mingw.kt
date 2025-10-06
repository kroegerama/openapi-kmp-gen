package com.kroegerama.openapi.kmp.gen.companion

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.winhttp.WinHttp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import platform.windows.GetVersionExW
import platform.windows.OSVERSIONINFOW

@OptIn(ExperimentalForeignApi::class)
internal actual val platformUserAgent: String = memScoped {
    val versionInfo = alloc<OSVERSIONINFOW>()
    versionInfo.dwOSVersionInfoSize = sizeOf<OSVERSIONINFOW>().toUInt()
    GetVersionExW(versionInfo.ptr)
    "WinHTTP Windows/${versionInfo.dwMajorVersion}.${versionInfo.dwMinorVersion}.${versionInfo.dwBuildNumber}"
}

internal actual fun createBaseClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    return HttpClient(WinHttp) {
        block()
    }
}
