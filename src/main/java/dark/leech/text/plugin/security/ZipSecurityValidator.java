package dark.leech.text.plugin.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Security validator for ZIP file operations. Prevents ZIP Slip attacks and enforces size limits.
 */
public class ZipSecurityValidator {

    private static final long MAX_ZIP_SIZE_BYTES = 100 * 1024 * 1024; // 100MB
    private static final long MAX_UNCOMPRESSED_SIZE_BYTES = 500 * 1024 * 1024; // 500MB
    private static final int MAX_ENTRY_COUNT = 1000;

    /**
     * Validates a ZIP file for security threats.
     *
     * @param zipPath Path to ZIP file
     * @throws SecurityValidationException if validation fails
     */
    public static void validateZipFile(Path zipPath) throws SecurityValidationException {
        if (!Files.exists(zipPath)) {
            throw new SecurityValidationException("ZIP file not found: " + zipPath);
        }

        // Check ZIP file size
        try {
            long zipSize = Files.size(zipPath);
            if (zipSize > MAX_ZIP_SIZE_BYTES) {
                throw new SecurityValidationException(
                        String.format(
                                "ZIP file too large: %d MB (max: %d MB)",
                                zipSize / (1024 * 1024), MAX_ZIP_SIZE_BYTES / (1024 * 1024)));
            }
        } catch (IOException e) {
            throw new SecurityValidationException("Failed to check ZIP file size", e);
        }

        // Validate all entries
        try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
            validateZipEntries(zipFile);
        } catch (IOException e) {
            throw new SecurityValidationException("Failed to open ZIP file", e);
        }
    }

    /**
     * Validates all ZIP entries for path traversal and size limits. Uses proper path normalization
     * to prevent ZIP Slip attacks.
     *
     * @param zipFile Open ZipFile to validate
     * @throws SecurityValidationException if validation fails
     */
    public static void validateZipEntries(ZipFile zipFile) throws SecurityValidationException {
        int entryCount = 0;
        long totalUncompressedSize = 0;

        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            entryCount++;

            if (entryCount > MAX_ENTRY_COUNT) {
                throw new SecurityValidationException(
                        "ZIP file contains too many entries: "
                                + entryCount
                                + " (max: "
                                + MAX_ENTRY_COUNT
                                + ")");
            }

            // Check uncompressed size
            long uncompressedSize = entry.getSize();
            if (uncompressedSize > 0) {
                totalUncompressedSize += uncompressedSize;
                if (totalUncompressedSize > MAX_UNCOMPRESSED_SIZE_BYTES) {
                    throw new SecurityValidationException(
                            String.format(
                                    "Total uncompressed size too large: %d MB (max: %d MB)",
                                    totalUncompressedSize / (1024 * 1024),
                                    MAX_UNCOMPRESSED_SIZE_BYTES / (1024 * 1024)));
                }
            }

            // Validate entry name with path normalization
            validateEntryName(entry.getName());
        }
    }

    /**
     * Validates a ZIP entry name to prevent path traversal attacks. Uses Path normalization to
     * detect ZIP Slip attempts.
     *
     * @param entryName The ZIP entry name
     * @throws SecurityValidationException if path traversal detected
     */
    public static void validateEntryName(String entryName) throws SecurityValidationException {
        if (entryName == null || entryName.isEmpty()) {
            throw new SecurityValidationException("ZIP entry name is empty");
        }

        // Normalize the path and check if it escapes the current directory
        Path normalizedPath;
        try {
            normalizedPath = Paths.get(entryName).normalize();
        } catch (Exception e) {
            throw new SecurityValidationException("Invalid ZIP entry name: " + entryName, e);
        }

        // Check if normalized path starts with ".." (escapes current directory)
        if (normalizedPath.startsWith("..")) {
            throw new SecurityValidationException(
                    "ZIP entry escapes directory (path traversal): " + entryName);
        }

        // Check if normalized path is absolute
        if (normalizedPath.isAbsolute()) {
            throw new SecurityValidationException("ZIP entry contains absolute path: " + entryName);
        }

        // Additional check: look for obvious traversal patterns before normalization
        if (entryName.contains("../") || entryName.contains("..\\")) {
            throw new SecurityValidationException(
                    "ZIP entry contains path traversal sequence: " + entryName);
        }

        // Check for drive letter (Windows absolute path)
        if (entryName.matches("^[A-Za-z]:.*")) {
            throw new SecurityValidationException(
                    "ZIP entry contains Windows absolute path: " + entryName);
        }

        // Check for UNC paths
        if (entryName.startsWith("\\\\") || entryName.startsWith("//")) {
            throw new SecurityValidationException("ZIP entry contains UNC path: " + entryName);
        }
    }

    /**
     * Resolves a ZIP entry name safely against a base directory. Ensures the resolved path stays
     * within the base directory.
     *
     * @param baseDir Base directory
     * @param entryName ZIP entry name
     * @return Safe resolved path
     * @throws SecurityValidationException if path escapes base directory
     */
    public static Path resolveSafePath(Path baseDir, String entryName)
            throws SecurityValidationException {
        // Validate entry name first
        validateEntryName(entryName);

        // Resolve against base directory
        Path resolved = baseDir.resolve(entryName).normalize();

        // Ensure resolved path is still within base directory
        if (!resolved.startsWith(baseDir.normalize())) {
            throw new SecurityValidationException(
                    "ZIP entry path escapes base directory: " + entryName);
        }

        return resolved;
    }
}
