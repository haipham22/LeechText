package dark.leech.text.plugin.vbook;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.api.PluginFormat;
import dark.leech.text.plugin.api.PluginFormatException;
import dark.leech.text.plugin.api.PluginMetadata;
import dark.leech.text.plugin.api.ValidationResult;
import dark.leech.text.plugin.security.RegexSecurityValidator;
import dark.leech.text.plugin.security.SecurityValidationException;
import dark.leech.text.plugin.validation.SecurityScanner;
import dark.leech.text.plugin.vbook.model.VBookPluginEntity;

/** vBook plugin format handler. Converts vBook plugin format to standard LeechText format. */
public class VBookPluginFormat implements PluginFormat {

    private final RegexSecurityValidator regexValidator;
    private final SecurityScanner securityScanner;

    public VBookPluginFormat() {
        this.regexValidator = new RegexSecurityValidator();
        this.securityScanner = new SecurityScanner();
    }

    @Override
    public String getFormatType() {
        return "vbook-json";
    }

    @Override
    public PluginEntity parse(InputStream input) throws PluginFormatException {
        try {
            // Parse vBook format from JSON
            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            VBookPluginEntity vbookPlugin = parseVBookJson(json);

            // Convert to LeechText format
            return convertToLeechText(vbookPlugin);

        } catch (PluginFormatException e) {
            throw e;
        } catch (Exception e) {
            throw new PluginFormatException(
                    "Failed to parse vBook plugin: " + e.getMessage(),
                    PluginFormatException.ErrorType.PARSE_ERROR,
                    e);
        }
    }

    @Override
    public ValidationResult validate(PluginEntity plugin) {
        ValidationResult.Builder builder = ValidationResult.builder();

        // Validate required fields
        if (plugin.getName() == null || plugin.getName().isEmpty()) {
            builder.addError("Plugin name is required");
        }
        if (plugin.getRegex() == null || plugin.getRegex().isEmpty()) {
            builder.addError("Plugin regex is required");
        }

        // Validate regex patterns
        if (plugin.getRegex() != null) {
            try {
                RegexSecurityValidator.validateRegex(plugin.getRegex());
            } catch (SecurityValidationException e) {
                builder.addError("Invalid regex pattern: " + e.getMessage());
            }
        }

        // Check for at least one script
        boolean hasScript =
                (plugin.getChapGetter() != null && !plugin.getChapGetter().isEmpty())
                        || (plugin.getTocGetter() != null && !plugin.getTocGetter().isEmpty());

        if (!hasScript) {
            builder.addError("Plugin must have at least chapGetter or tocGetter");
        }

        // Security scan
        try {
            ValidationResult securityResult = securityScanner.validateSecurity(plugin);
            if (!securityResult.isValid()) {
                for (String error : securityResult.getErrors()) {
                    builder.addError(error);
                }
            }
            for (String warning : securityResult.getWarnings()) {
                builder.addWarning(warning);
            }
        } catch (Exception e) {
            builder.addWarning("Security scan failed: " + e.getMessage());
        }

        return builder.build();
    }

    @Override
    public PluginEntity normalize(PluginEntity plugin) {
        // vBook plugins are already in standard format after conversion
        // This is a no-op but kept for interface compliance
        return plugin;
    }

    @Override
    public PluginMetadata extractMetadata(InputStream input) throws PluginFormatException {
        try {
            String json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            VBookPluginEntity vbookPlugin = parseVBookJson(json);

            return dark.leech.text.plugin.vbook.model.VBookPluginMetadata.vbookBuilder()
                    .id(vbookPlugin.getName())
                    .name(vbookPlugin.getName())
                    .author(vbookPlugin.getAuthor())
                    .version(String.valueOf(vbookPlugin.getVersion()))
                    .description(vbookPlugin.getDescription())
                    .source("vbook")
                    .iconUrl(null) // vBookPluginEntity has iconBase64, not iconUrl
                    .type("vbook")
                    .group(vbookPlugin.getType())
                    .language(vbookPlugin.getLanguage())
                    .build();
        } catch (Exception e) {
            throw new PluginFormatException(
                    "Failed to extract metadata: " + e.getMessage(),
                    PluginFormatException.ErrorType.PARSE_ERROR,
                    e);
        }
    }

    /**
     * Parse vBook JSON format. Note: VBookPluginEntity doesn't have fromJson method. This should be
     * handled by VBookRepositoryClient or PluginZipExtractor.
     */
    private VBookPluginEntity parseVBookJson(String json) throws PluginFormatException {
        try {
            // VBookPluginEntity parsing is handled by VBookRepositoryClient
            // This method is a placeholder for the format interface
            // In practice, use PluginZipExtractor which has the actual JSON parsing logic
            throw new PluginFormatException(
                    "Direct JSON parsing not supported - use PluginZipExtractor instead",
                    PluginFormatException.ErrorType.PARSE_ERROR);
        } catch (Exception e) {
            throw new PluginFormatException(
                    "Failed to parse vBook JSON: " + e.getMessage(),
                    PluginFormatException.ErrorType.PARSE_ERROR,
                    e);
        }
    }

    /** Convert vBook format to LeechText PluginEntity using the converter. */
    private PluginEntity convertToLeechText(VBookPluginEntity vbookPlugin) {
        VBookToLeechTextConverter converter = new VBookToLeechTextConverter();
        return converter.convert(vbookPlugin);
    }
}
