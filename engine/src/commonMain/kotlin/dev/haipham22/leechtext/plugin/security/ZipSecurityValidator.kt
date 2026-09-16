package dev.haipham22.leechtext.plugin.security

import dev.haipham22.leechtext.log.EngineLogger
import dev.haipham22.leechtext.util.ZipIo
import okio.FileSystem
import okio.Path

/**
 * Validator ZIP chống ZIP Slip + size limit (port từ ZipSecurityValidator.java; P5.2c:
 * java.util.zip.ZipFile → ZipIo seam, entry count/size giữ nguyên).
 */
object ZipSecurityValidator {
    private const val MAX_ZIP_SIZE_BYTES = 100L * 1024 * 1024 // 100MB
    private const val MAX_UNCOMPRESSED_SIZE_BYTES = 500L * 1024 * 1024 // 500MB
    private const val MAX_ENTRY_COUNT = 1000

    /** Validate ZIP file cho security threats. */
    @Throws(SecurityValidationException::class)
    @Suppress("ThrowsCount") // validation chain ở trust boundary — mỗi guard ném exception có message riêng cho UI
    fun validateZipFile(
        zipPath: Path,
        log: EngineLogger,
    ) {
        if (!FileSystem.SYSTEM.exists(zipPath)) {
            throw SecurityValidationException("ZIP file not found: $zipPath")
        }

        // Check kích thước file zip
        val zipSize =
            try {
                FileSystem.SYSTEM.metadata(zipPath).size ?: 0L
            } catch (e: Exception) {
                throw SecurityValidationException("Failed to check ZIP file size", e)
            }
        if (zipSize > MAX_ZIP_SIZE_BYTES) {
            throw SecurityValidationException(
                "ZIP file too large: ${zipSize / (1024 * 1024)} MB" +
                    " (max: ${MAX_ZIP_SIZE_BYTES / (1024 * 1024)} MB)",
            )
        }

        // Validate mọi entry
        try {
            validateZipEntries(zipPath, log)
        } catch (e: SecurityValidationException) {
            throw e
        } catch (e: Exception) {
            throw SecurityValidationException("Failed to open ZIP file", e)
        }
    }

    /**
     * Validate mọi ZIP entry cho path traversal + size limit. Path normalization chống ZIP
     * Slip.
     */
    @Throws(SecurityValidationException::class)
    @Suppress("ThrowsCount") // validation chain ở trust boundary — mỗi guard ném exception có message riêng cho UI
    fun validateZipEntries(
        zipPath: Path,
        log: EngineLogger,
    ) {
        var entryCount = 0
        var totalUncompressedSize = 0L

        val entries =
            try {
                ZipIo.entries(zipPath, log)
            } catch (e: Exception) {
                throw SecurityValidationException("Failed to read ZIP entries", e)
            }

        for (entry in entries) {
            entryCount++

            if (entryCount > MAX_ENTRY_COUNT) {
                throw SecurityValidationException("ZIP file contains too many entries: $entryCount (max: $MAX_ENTRY_COUNT)")
            }

            // Check uncompressed size
            if (entry.size > 0) {
                totalUncompressedSize += entry.size
                if (totalUncompressedSize > MAX_UNCOMPRESSED_SIZE_BYTES) {
                    throw SecurityValidationException(
                        "Total uncompressed size too large: " +
                            "${totalUncompressedSize / (1024 * 1024)} MB" +
                            " (max: ${MAX_UNCOMPRESSED_SIZE_BYTES / (1024 * 1024)} MB)",
                    )
                }
            }

            // Validate entry name với path normalization
            validateEntryName(entry.name)
        }
    }

    /** Validate ZIP entry name chống path traversal (ZIP Slip). */
    @Throws(SecurityValidationException::class)
    @Suppress("ThrowsCount") // validation chain ở trust boundary — mỗi guard ném exception có message riêng cho UI
    fun validateEntryName(entryName: String?) {
        if (entryName.isNullOrEmpty()) {
            throw SecurityValidationException("ZIP entry name is empty")
        }

        // Normalize path (bỏ ./ và resolve ../), check escape khỏi current directory
        val normalizedPath = entryName.replace("\\", "/")
        val segments = normalizedPath.split("/")
        var depth = 0
        for (seg in segments) {
            when (seg) {
                // segment rỗng (split "//") và "." — không đổi depth
                "", "." -> depth += 0

                ".." -> {
                    depth--
                    if (depth < 0) {
                        throw SecurityValidationException("ZIP entry escapes directory (path traversal): $entryName")
                    }
                }

                else -> depth++
            }
        }

        // Absolute path posix
        if (normalizedPath.startsWith("/")) {
            throw SecurityValidationException("ZIP entry contains absolute path: $entryName")
        }

        // Drive letter (Windows absolute path)
        if (entryName.matches(Regex("^[A-Za-z]:.*"))) {
            throw SecurityValidationException("ZIP entry contains Windows absolute path: $entryName")
        }

        // UNC path
        if (entryName.startsWith("\\\\") || entryName.startsWith("//")) {
            throw SecurityValidationException("ZIP entry contains UNC path: $entryName")
        }
    }

    /**
     * Resolve ZIP entry name an toàn với base dir — đảm bảo path nằm trong base dir.
     */
    @Throws(SecurityValidationException::class)
    fun resolveSafePath(
        baseDir: Path,
        entryName: String,
    ): Path {
        // Validate entry name trước
        validateEntryName(entryName)

        // Resolve với base directory (entryName đã validate không chứa ..)
        val resolved = baseDir / entryName

        // Đảm bảo vẫn nằm trong base directory
        if (!resolved.toString().startsWith(baseDir.toString())) {
            throw SecurityValidationException("ZIP entry path escapes base directory: $entryName")
        }

        return resolved
    }
}
