package dark.leech.text.plugin.vbook;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;

import dark.leech.text.action.Log;
import dark.leech.text.plugin.security.NetworkSecurityValidator;
import dark.leech.text.plugin.security.RegexSecurityValidator;
import dark.leech.text.plugin.security.SecurityValidationException;
import dark.leech.text.plugin.security.ZipSecurityValidator;
import dark.leech.text.plugin.vbook.exception.VBookPluginException;
import dark.leech.text.plugin.vbook.model.VBookPluginEntity;
import dark.leech.text.util.FileUtils;

/**
 * Extracts and parses vBook plugin.zip files. Downloads ZIP from URL and extracts plugin.json,
 * scripts, and icon.
 */
public class PluginZipExtractor {

    private static final String DEFAULT_CACHE_DIR = "tools/plugins/vbook-cache";
    private static final Gson GSON = new Gson();

    /**
     * Extract plugin from local ZIP file.
     *
     * @param zipPath Path to plugin.zip file
     * @return VBookPluginEntity with extracted data
     * @throws VBookPluginException if extraction fails
     */
    public VBookPluginEntity extractFromZip(Path zipPath) throws VBookPluginException {
        File zipFile = zipPath.toFile();
        if (!zipFile.exists()) {
            throw new VBookPluginException("ZIP file not found: " + zipPath);
        }

        // Validate ZIP file for security threats (after existence check)
        try {
            ZipSecurityValidator.validateZipFile(zipPath);
        } catch (SecurityValidationException e) {
            throw new VBookPluginException("ZIP security validation failed: " + e.getMessage(), e);
        }

        try {
            return extractZipInternal(zipFile);
        } catch (VBookPluginException e) {
            // Re-throw VBookPluginException as-is
            throw e;
        } catch (Exception e) {
            throw new VBookPluginException("Failed to extract plugin from: " + zipPath, e);
        }
    }

    /**
     * Download and extract plugin from URL.
     *
     * @param zipUrl URL to plugin.zip file
     * @return VBookPluginEntity with extracted data
     * @throws VBookPluginException if download or extraction fails
     */
    public VBookPluginEntity extractFromZip(URL zipUrl) throws VBookPluginException {
        try {
            // Download to cache
            Path cacheDir = Paths.get(FileUtils.validate(DEFAULT_CACHE_DIR));
            Files.createDirectories(cacheDir);

            String filename = extractFilenameFromUrl(zipUrl.toString());
            Path localZipPath = cacheDir.resolve(filename);

            // Remove existing file if exists (forces fresh download)
            try {
                Files.deleteIfExists(localZipPath);
            } catch (IOException ignored) {
            }

            downloadFile(zipUrl, localZipPath);
            return extractFromZip(localZipPath);

        } catch (Exception e) {
            throw new VBookPluginException("Failed to download plugin from: " + zipUrl, e);
        }
    }

    /**
     * Internal extraction logic using zip4j. Includes ZIP Slip protection to prevent path traversal
     * attacks.
     */
    private VBookPluginEntity extractZipInternal(File zipFile) throws Exception {
        ZipFile zip = new ZipFile(zipFile);

        // Validate ZIP entries for path traversal (ZIP Slip protection)
        validateZipEntries(zip);

        // Extract plugin.json (metadata)
        String pluginJson = extractEntryAsString(zip, "plugin.json");
        if (pluginJson == null) {
            throw new VBookPluginException("plugin.json not found in ZIP");
        }

        // Parse plugin.json and create entity
        VBookPluginEntity entity = parsePluginMetadata(pluginJson);

        // Validate regex pattern from metadata
        if (entity.getRegexp() != null) {
            validateRegexPattern(entity.getRegexp());
        }

        // Extract scripts from src/ directory
        Map<String, String> scriptContents = extractScripts(zip);
        entity.setScriptContents(scriptContents);

        // Extract icon.png
        String iconBase64 = extractIconAsBase64(zip);
        if (iconBase64 != null) {
            entity.setIconBase64(iconBase64);
        }

        // Set raw metadata
        entity.setRawMetadata(pluginJson);

        // Validate required scripts
        if (!scriptContents.containsKey("chap") && !scriptContents.containsKey("toc") && !scriptContents.containsKey("gen")) {
            throw new VBookPluginException("Plugin must have at least chap.js or toc.js");
        }

        return entity;
    }

    /**
     * Validate ZIP entries to prevent ZIP Slip path traversal attacks. Uses ZipSecurityValidator
     * for comprehensive security checks.
     */
    private void validateZipEntries(ZipFile zip) throws VBookPluginException {
        try {
            for (net.lingala.zip4j.model.FileHeader header : zip.getFileHeaders()) {
                String fileName = header.getFileName();
                ZipSecurityValidator.validateEntryName(fileName);
            }
        } catch (SecurityValidationException e) {
            throw new VBookPluginException("ZIP entry validation failed", e);
        } catch (Exception e) {
            throw new VBookPluginException("Failed to validate ZIP entries", e);
        }
    }

    /**
     * Validate regex pattern to prevent ReDoS attacks. Uses RegexSecurityValidator for
     * comprehensive timeout and pattern checks.
     */
    private void validateRegexPattern(String regex) throws VBookPluginException {
        try {
            RegexSecurityValidator.validateRegex(regex);
        } catch (SecurityValidationException e) {
            throw new VBookPluginException("Regex validation failed", e);
        }
    }

    /** Parse plugin.json metadata into entity. */
    private VBookPluginEntity parsePluginMetadata(String json) {
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);

            // Create entity with default values
            VBookPluginEntity entity = new VBookPluginEntity();
            entity.setScripts(new HashMap<>());
            entity.setScriptContents(new HashMap<>());

            // Parse metadata section
            JsonObject metadata = root.getAsJsonObject("metadata");
            if (metadata != null) {
                entity.setName(getString(metadata, "name"));
                entity.setAuthor(getString(metadata, "author"));
                entity.setVersion(getInt(metadata, "version"));
                entity.setSource(getString(metadata, "source"));
                entity.setRegexp(getString(metadata, "regexp"));
                entity.setDescription(getString(metadata, "description"));
                entity.setLocale(getString(metadata, "locale"));
                entity.setType(getString(metadata, "type"));
                entity.setLanguage(getString(metadata, "language"));
                entity.setPriority(getInt(metadata, "priority"));
                entity.setTag(getString(metadata, "tag"));
            }

            // Parse script section (file references)
            JsonObject scripts = root.getAsJsonObject("script");
            if (scripts != null) {
                Map<String, String> scriptMap = new HashMap<>();
                scripts.entrySet()
                        .forEach(
                                entry -> {
                                    scriptMap.put(entry.getKey(), entry.getValue().getAsString());
                                });
                entity.setScripts(scriptMap);
            }

            // Parse config section if present
            JsonObject config = root.getAsJsonObject("config");
            if (config != null) {
                // Config options can be stored in description for now
                String configStr = config.toString();
                String currentDesc = entity.getDescription() != null ? entity.getDescription() : "";
                entity.setDescription(currentDesc + "\n\nConfig: " + configStr);
            }

            return entity;

        } catch (JsonSyntaxException e) {
            throw new VBookPluginException("Failed to parse plugin.json", e);
        }
    }

    /** Extract all .js files from src/ directory. */
    private Map<String, String> extractScripts(ZipFile zip) throws Exception {
        Map<String, String> scripts = new HashMap<>();
        Pattern scriptPattern = Pattern.compile("^src/([^/]+)\\.js$");

        List<FileHeader> headers = zip.getFileHeaders();
        for (FileHeader header : headers) {
            String entryName = header.getFileName();

            // Skip directories
            if (header.isDirectory()) {
                continue;
            }

            // Match src/*.js files
            Matcher matcher = scriptPattern.matcher(entryName);
            if (matcher.matches()) {
                String scriptName = matcher.group(1);
                String content = extractEntryAsString(zip, entryName);
                if (content != null) {
                    scripts.put(scriptName, content);
                }
            }
        }

        return scripts;
    }

    /** Extract icon.png and convert to base64 data URI. */
    private String extractIconAsBase64(ZipFile zip) {
        try {
            List<FileHeader> headers = zip.getFileHeaders();
            for (FileHeader header : headers) {
                if ("icon.png".equals(header.getFileName()) && !header.isDirectory()) {
                    try (InputStream is = zip.getInputStream(header)) {
                        byte[] bytes = is.readAllBytes();
                        String base64 = Base64.getEncoder().encodeToString(bytes);
                        return "data:image/png;base64," + base64;
                    }
                }
            }
        } catch (Exception e) {
            // Icon extraction is optional - log but don't fail
            Log.add("Failed to extract icon (non-critical): " + e.getMessage());
        }
        return null;
    }

    /** Extract a single entry from ZIP as string. */
    private String extractEntryAsString(ZipFile zip, String entryName) throws Exception {
        FileHeader header = zip.getFileHeader(entryName);
        if (header == null || header.isDirectory()) {
            return null;
        }

        InputStream is = zip.getInputStream(header);
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Download file from URL to local path with security validation. */
    private void downloadFile(URL url, Path destPath) throws IOException {
        // Validate URL before download
        try {
            NetworkSecurityValidator.validateUrl(url.toString());
        } catch (SecurityValidationException e) {
            throw new IOException("URL validation failed: " + e.getMessage(), e);
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        connection.setInstanceFollowRedirects(true);

        // Validate response headers
        String contentType = connection.getContentType();
        long contentLength = connection.getContentLengthLong();

        try {
            NetworkSecurityValidator.validateResponse(contentType, contentLength);
        } catch (SecurityValidationException e) {
            connection.disconnect();
            throw new IOException("Response validation failed: " + e.getMessage(), e);
        }

        // Download to temporary file first
        Path tempPath = destPath.getParent().resolve(destPath.getFileName().toString() + ".tmp");
        try {
            try (InputStream is = connection.getInputStream()) {
                Files.copy(is, tempPath, StandardCopyOption.REPLACE_EXISTING);
            }

            // Atomic move from temp to final destination
            Files.move(
                    tempPath,
                    destPath,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            // Clean up temp file on failure
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {
                // Ignore cleanup failures
            }
            throw e;
        } finally {
            connection.disconnect();
        }
    }

    /** Extract filename from URL. */
    private String extractFilenameFromUrl(String url) {
        String[] parts = url.split("/");
        return parts[parts.length - 1];
    }

    // Helper methods for JSON parsing

    private String getString(JsonObject obj, String name) {
        if (obj.has(name) && !obj.get(name).isJsonNull()) {
            return obj.get(name).getAsString();
        }
        return null;
    }

    private Integer getInt(JsonObject obj, String name) {
        if (obj.has(name) && !obj.get(name).isJsonNull()) {
            return obj.get(name).getAsInt();
        }
        return null;
    }
}
