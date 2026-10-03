package com.kroegerama.openapi.kmp.gen.companion

import com.kroegerama.openapi.kmp.gen.BuildConfig

public val defaultUserAgent: String
    get() = "ktor/${BuildConfig.KTOR} kmp-gen/${BuildConfig.COMPANION} $platformUserAgent"

public expect val platformUserAgent: String
