package dark.leech.text.plugin.api;

import java.io.InputStream;

import dark.leech.text.enities.PluginEntity;

/**
 * Abstraction for plugin file format handling. Different repositories may use different plugin
 * formats.
 */
public interface PluginFormat {

    /** Format identifier. Examples: "vbook-json", "leechtext-json", "zip" */
    String getFormatType();

    /**
     * Parse plugin from input stream. Validates format during parsing.
     *
     * @param input Input stream containing plugin data
     * @return Parsed plugin entity
     * @throws PluginFormatException if parsing fails
     */
    PluginEntity parse(InputStream input) throws PluginFormatException;

    /**
     * Validate plugin before installation. Checks required fields, script syntax, security
     * constraints.
     *
     * @param plugin Plugin to validate
     * @return Validation result with details
     */
    ValidationResult validate(PluginEntity plugin);

    /**
     * Convert plugin to standard LeechText format if needed. Some formats may require field mapping
     * or transformation.
     *
     * @param plugin Plugin in source format
     * @return Normalized plugin entity
     */
    PluginEntity normalize(PluginEntity plugin);

    /**
     * Extract metadata from plugin without full parsing. Used for search results and listings.
     *
     * @param input Input stream with partial plugin data
     * @return Plugin metadata only
     * @throws PluginFormatException if metadata extraction fails
     */
    PluginMetadata extractMetadata(InputStream input) throws PluginFormatException;
}
