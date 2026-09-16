package dev.haipham22.leechtext.util

/** SHA-256 hex (lowercase) — actual per-platform (JVM MessageDigest, apple CommonCrypto). */
expect fun sha256Hex(bytes: ByteArray): String
