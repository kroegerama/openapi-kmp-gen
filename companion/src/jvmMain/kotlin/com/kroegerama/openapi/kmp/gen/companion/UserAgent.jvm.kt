package com.kroegerama.openapi.kmp.gen.companion

public actual val platformUserAgent: String = run {
    val osName = System.getProperty("os.name") ?: "unknown"
    val osVersion = System.getProperty("os.version") ?: "unknown"
    "okhttp/${okhttp3.OkHttp.VERSION} $osName/$osVersion"
}
