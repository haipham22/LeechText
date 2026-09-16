package dev.haipham22.leechtext.util

import platform.Foundation.NSString
import platform.Foundation.precomposedStringWithCanonicalMapping

actual fun String.nfc(): String = (this as NSString).precomposedStringWithCanonicalMapping() as String
