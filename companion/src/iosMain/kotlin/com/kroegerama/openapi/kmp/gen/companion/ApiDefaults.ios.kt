package com.kroegerama.openapi.kmp.gen.companion

import platform.UIKit.UIDevice

internal actual val platformUserAgent: String = UIDevice.currentDevice.run {
    "Darwin $systemName/$systemVersion"
}
