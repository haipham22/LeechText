package dark.leech.text.plugin.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.api.ValidationResult;
import dark.leech.text.plugin.sandbox.PluginSandbox;
import dark.leech.text.plugin.security.RegexSecurityValidator;
import dark.leech.text.plugin.security.SecurityConstants;
import dark.leech.text.plugin.security.SecurityValidationException;

/** Scans plugin scripts for malicious patterns. Implements plugin validation interface. */
public class SecurityScanner implements PluginValidator {

    // Critical malicious patterns - these will cause plugin rejection
    private static final Pattern[] CRITICAL_PATTERNS = {
        Pattern.compile("Runtime\\.getRuntime\\(\\)\\.exec", Pattern.CASE_INSENSITIVE),
        Pattern.compile("ProcessBuilder", Pattern.CASE_INSENSITIVE),
        Pattern.compile("System\\.exit", Pattern.CASE_INSENSITIVE),
        Pattern.compile("Class\\.forName", Pattern.CASE_INSENSITIVE),
        Pattern.compile("new\\s+File\\s*\\(\\s*['\"].*\\.\\./", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.exec\\s*\\(\\s*['\"]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("eval\\s*\\(\\s*['\"]\\$\\{", Pattern.CASE_INSENSITIVE),
        Pattern.compile("<\\?php", Pattern.CASE_INSENSITIVE)
    };

    // High severity suspicious patterns
    private static final Pattern[] HIGH_SEVERITY_PATTERNS = {
        Pattern.compile("while\\s*\\(\\s*true\\s*\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("for\\s*\\(\\s*;.*;.*\\)\\s*\\{", Pattern.CASE_INSENSITIVE),
        Pattern.compile("new\\s+Array\\s*\\(\\s*\\d{4,}\\s*\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.load\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("import.*powershell", Pattern.CASE_INSENSITIVE)
    };

    // Medium severity warning patterns
    private static final Pattern[] WARNING_PATTERNS = {
        Pattern.compile("while\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("\\.exec\\s*\\(", Pattern.CASE_INSENSITIVE),
        Pattern.compile("document\\.write", Pattern.CASE_INSENSITIVE),
        Pattern.compile("innerHTML\\s*\\+", Pattern.CASE_INSENSITIVE)
    };

    @Override
    public ValidationResult validateSecurity(PluginEntity plugin) {
        ValidationResult.Builder builder = ValidationResult.builder();

        // Check required fields
        if (plugin.getName() == null || plugin.getName().isEmpty()) {
            builder.addError("Plugin name is required");
        }

        if (plugin.getRegex() == null || plugin.getRegex().isEmpty()) {
            builder.addError("Plugin regex pattern is required");
        } else {
            // Validate regex pattern using security validator
            try {
                RegexSecurityValidator.validateRegex(plugin.getRegex());
            } catch (SecurityValidationException e) {
                builder.addError("Invalid regex pattern: " + e.getMessage());
            }
        }

        // Check for at least one script
        boolean hasScript = hasScriptContent(plugin);
        if (!hasScript) {
            builder.addError("Plugin must have at least one script (chap or toc)");
        }

        // Scan all script content for security issues
        String combinedScripts = combineAllScripts(plugin);

        List<SecurityFinding> findings = scanForMalware(combinedScripts);
        for (SecurityFinding finding : findings) {
            if (finding.getSeverity() == SecurityFinding.Severity.CRITICAL
                    || finding.getSeverity() == SecurityFinding.Severity.HIGH) {
                builder.addError(finding.getDescription());
            } else if (finding.getSeverity() == SecurityFinding.Severity.MEDIUM) {
                builder.addWarning(finding.getDescription());
            }
        }

        return builder.build();
    }

    @Override
    public List<SecurityFinding> scanForMalware(String scriptContent) {
        List<SecurityFinding> findings = new ArrayList<>();

        if (scriptContent == null || scriptContent.isEmpty()) {
            return findings;
        }

        // Scan for critical malicious patterns
        for (Pattern pattern : CRITICAL_PATTERNS) {
            if (pattern.matcher(scriptContent).find()) {
                findings.add(
                        new SecurityFinding(
                                "Malicious pattern detected: " + pattern.pattern(),
                                SecurityFinding.Severity.CRITICAL,
                                SecurityFinding.Category.MALWARE));
            }
        }

        // Scan for high severity patterns
        for (Pattern pattern : HIGH_SEVERITY_PATTERNS) {
            if (pattern.matcher(scriptContent).find()) {
                findings.add(
                        new SecurityFinding(
                                "Suspicious pattern detected: " + pattern.pattern(),
                                SecurityFinding.Severity.HIGH,
                                SecurityFinding.Category.SUSPICIOUS));
            }
        }

        // Scan for warning patterns
        for (Pattern pattern : WARNING_PATTERNS) {
            if (pattern.matcher(scriptContent).find()) {
                findings.add(
                        new SecurityFinding(
                                "Code quality warning: " + pattern.pattern(),
                                SecurityFinding.Severity.MEDIUM,
                                SecurityFinding.Category.CODE_QUALITY));
            }
        }

        return findings;
    }

    @Override
    public boolean verifySignature(PluginEntity plugin, byte[] signature) {
        // Signature verification deferred to v2.1
        // For now, we don't support plugin signing
        // Note: Avoid Log.add() here to prevent headless exception in tests
        return false;
    }

    @Override
    public ValidationResult testInSandbox(PluginEntity plugin, PluginSandbox sandbox) {
        if (sandbox == null) {
            return ValidationResult.failure("Sandbox not available for testing");
        }

        if (!sandbox.isEnabled()) {
            return ValidationResult.success(List.of("Sandbox testing is disabled"));
        }

        return sandbox.test(plugin);
    }

    /** Check if plugin has any script content. */
    private boolean hasScriptContent(PluginEntity plugin) {
        return (plugin.getChapGetter() != null && !plugin.getChapGetter().isEmpty())
                || (plugin.getTocGetter() != null && !plugin.getTocGetter().isEmpty())
                || (plugin.getPageGetter() != null && !plugin.getPageGetter().isEmpty())
                || (plugin.getSearchGetter() != null && !plugin.getSearchGetter().isEmpty())
                || (plugin.getDetailGetter() != null && !plugin.getDetailGetter().isEmpty());
    }

    /** Combine all script content into a single string for scanning. */
    private String combineAllScripts(PluginEntity plugin) {
        StringBuilder sb = new StringBuilder();

        appendScript(sb, plugin.getChapGetter());
        appendScript(sb, plugin.getTocGetter());
        appendScript(sb, plugin.getPageGetter());
        appendScript(sb, plugin.getSearchGetter());
        appendScript(sb, plugin.getDetailGetter());

        return sb.toString();
    }

    private void appendScript(StringBuilder sb, String script) {
        if (script != null && !script.isEmpty()) {
            sb.append(script);
            sb.append("\n");
        }
    }

    /**
     * Check if API call is allowed.
     *
     * @param apiName The API name to check
     * @return true if allowed
     */
    public boolean isApiAllowed(String apiName) {
        return SecurityConstants.isApiAllowed(apiName);
    }
}
