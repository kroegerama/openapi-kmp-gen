package com.kroegerama.openapi.kmp.gen.companion

import platform.Foundation.NSProcessInfo

internal actual val platformUserAgent: String = NSProcessInfo.processInfo.run {
    "Darwin ${operatingSystemName()}/$operatingSystemVersionString"
}
