package dev.haipham22.leechtext.util

import java.security.MessageDigest

/** JVM actual — MessageDigest SHA-256, hex lowercase. */
actual fun sha256Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}
