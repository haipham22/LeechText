package dev.haipham22.leechtext.util

import java.text.Normalizer

actual fun String.nfc(): String = Normalizer.normalize(this, Normalizer.Form.NFC)
