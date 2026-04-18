package dark.leech.text.plugin.vbook;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.vbook.model.VBookPluginEntity;

/**
 * Converts vBook plugin format to LeechText PluginEntity format. Handles metadata mapping, script
 * inlining, and format transformations.
 */
public class VBookToLeechTextConverter {

    /**
     * Convert a vBook plugin to LeechText PluginEntity.
     *
     * @param vbookPlugin The vBook plugin to convert
     * @return PluginEntity in LeechText format
     * @throws IllegalArgumentException if required fields are missing
     */
    public PluginEntity convert(VBookPluginEntity vbookPlugin) {
        if (vbookPlugin == null) {
            throw new IllegalArgumentException("vbookPlugin cannot be null");
        }

        validateRequiredFields(vbookPlugin);

        // Build plugin entity using chained builder methods
        var entity =
                PluginEntity.builder()
                        .name(vbookPlugin.getName())
                        .author(vbookPlugin.getAuthor() != null ? vbookPlugin.getAuthor() : "vBook")
                        .version(convertVersion(vbookPlugin.getVersion()))
                        .source(vbookPlugin.getSource())
                        .regex(vbookPlugin.getRegexp())
                        .describe(vbookPlugin.getDescription())
                        .language(mapLocaleToLanguage(vbookPlugin.getLocale()))
                        .group(mapTypeToGroup(vbookPlugin.getType()))
                        .icon(vbookPlugin.getIconBase64())
                        .supportUpdate(true)
                        .checked(true)
                        .uuid(generateUuid(vbookPlugin.getName(), vbookPlugin.getVersion()));

        // Inline script contents
        Map<String, String> contents = vbookPlugin.getScriptContents();
        if (contents != null) {
            if (contents.containsKey("chap")) {
                entity.chapGetter(contents.get("chap"));
            }
            if (contents.containsKey("toc")) {
                entity.tocGetter(contents.get("toc"));
            }
            if (contents.containsKey("detail")) {
                entity.detailGetter(contents.get("detail"));
            }
            if (contents.containsKey("page")) {
                entity.pageGetter(contents.get("page"));
            }
            if (contents.containsKey("search")) {
                entity.searchGetter(contents.get("search"));
            }

            // Map vBook-specific scripts to appropriate getters or skip
            // home → Not used in LeechText, can add to description
            // genre → Not used in LeechText, can add to description
            // site → Not used in LeechText, can add to description
        }

        return entity.build();
    }

    /** Validate that required fields are present. */
    private void validateRequiredFields(VBookPluginEntity plugin) {
        if (plugin.getName() == null || plugin.getName().isEmpty()) {
            throw new IllegalArgumentException("Plugin name is required");
        }
        if (plugin.getRegexp() == null || plugin.getRegexp().isEmpty()) {
            throw new IllegalArgumentException("Plugin regexp is required");
        }
        Map<String, String> contents = plugin.getScriptContents();
        if (contents == null || contents.isEmpty()) {
            throw new IllegalArgumentException("Plugin must have at least one script");
        }
    }

    /**
     * Convert vBook integer version to LeechText double version. vBook uses integers like 10,
     * LeechText uses doubles like 1.0 Formula: int_version / 10.0
     *
     * @param vbookVersion Integer version from vBook
     * @return Double version for LeechText
     */
    private double convertVersion(Integer vbookVersion) {
        if (vbookVersion == null) {
            return 1.0;
        }
        return vbookVersion / 10.0;
    }

    /**
     * Map vBook locale to LeechText language code. vBook uses full locale (vi_VN), LeechText uses
     * short codes (vi)
     *
     * @param locale Locale from vBook (e.g., "vi_VN")
     * @return Language code for LeechText (e.g., "vi")
     */
    private String mapLocaleToLanguage(String locale) {
        if (locale == null || locale.isEmpty()) {
            return "en";
        }

        // Extract language code from locale (vi_VN → vi)
        String[] parts = locale.split("_");
        String langCode = parts[0].toLowerCase();

        // Map common language codes
        return switch (langCode) {
            case "vi" -> "vi";
            case "en" -> "en";
            case "zh" -> "cn";
            default -> langCode;
        };
    }

    /**
     * Map vBook type to LeechText group. vBook uses "novel" or "comic", LeechText uses group names
     *
     * @param type Type from vBook ("novel", "comic")
     * @return Group name for LeechText
     */
    private String mapTypeToGroup(String type) {
        if (type == null || type.isEmpty()) {
            return "dich"; // default group
        }

        return switch (type.toLowerCase()) {
            case "novel" -> "dich";
            case "comic" -> "truyentranh";
            default -> "dich";
        };
    }

    /**
     * Generate UUID from plugin name and version. Creates a consistent UUID based on SHA-256 hash.
     *
     * @param name Plugin name
     * @param version Plugin version
     * @return UUID string
     */
    private String generateUuid(String name, Integer version) {
        try {
            String input = name + "-" + (version != null ? version : "1");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Convert first 16 bytes to UUID format
            HexFormat hex = HexFormat.of();
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < 16; i++) {
                if (i == 4 || i == 6 || i == 8 || i == 10) {
                    sb.append("-");
                }
                sb.append(hex.toHexDigits(hash[i]));
            }

            return sb.toString();

        } catch (Exception e) {
            // Fallback to simple hash-based UUID
            return String.format(
                    "%08x-%04x-%04x-%04x-%012x",
                    name.hashCode(),
                    version != null ? version : 1,
                    0,
                    0,
                    System.currentTimeMillis() & 0xffffffffffffL);
        }
    }
}
