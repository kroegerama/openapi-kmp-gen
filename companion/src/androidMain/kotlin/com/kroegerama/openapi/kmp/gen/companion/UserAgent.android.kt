package com.kroegerama.openapi.kmp.gen.companion

import android.os.Build

public actual val platformUserAgent: String = run {
    "okhttp/${okhttp3.OkHttp.VERSION} Android/API ${Build.VERSION.SDK_INT}"
}
