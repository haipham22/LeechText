package dev.haipham22.leechtext.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

/** Apple actual — CommonCrypto CC_SHA256, hex lowercase. */
@OptIn(ExperimentalForeignApi::class)
actual fun sha256Hex(bytes: ByteArray): String {
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)
    bytes.usePinned { input ->
        digest.usePinned { output ->
            CC_SHA256(input.addressOf(0), bytes.size.toUInt(), output.addressOf(0))
        }
    }
    return digest.joinToString("") { it.toString(16).padStart(2, '0') }
}
