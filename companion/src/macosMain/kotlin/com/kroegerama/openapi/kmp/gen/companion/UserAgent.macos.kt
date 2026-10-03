package com.kroegerama.openapi.kmp.gen.companion

import platform.Foundation.NSProcessInfo

public actual val platformUserAgent: String = NSProcessInfo.processInfo.run {
    "Darwin macOS/$operatingSystemVersionString"
}
