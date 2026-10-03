package com.kroegerama.openapi.kmp.gen.companion

import platform.UIKit.UIDevice

public actual val platformUserAgent: String = UIDevice.currentDevice.run {
    "Darwin $systemName/$systemVersion"
}
