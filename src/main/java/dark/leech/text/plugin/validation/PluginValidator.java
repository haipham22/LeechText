package dark.leech.text.plugin.validation;

import java.util.List;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.api.ValidationResult;
import dark.leech.text.plugin.sandbox.PluginSandbox;

/** Validates plugins before installation. Checks security, syntax, and compliance. */
public interface PluginValidator {

    /**
     * Validate plugin security. Checks for malicious patterns, unsafe APIs, etc.
     *
     * @param plugin Plugin to validate
     * @return Validation result
     */
    ValidationResult validateSecurity(PluginEntity plugin);

    /**
     * Scan plugin script content for malware.
     *
     * @param scriptContent Script content to scan
     * @return List of security findings
     */
    List<SecurityFinding> scanForMalware(String scriptContent);

    /**
     * Verify plugin signature if present.
     *
     * @param plugin Plugin to verify
     * @param signature Signature bytes
     * @return true if signature valid
     */
    boolean verifySignature(PluginEntity plugin, byte[] signature);

    /**
     * Test plugin in sandbox.
     *
     * @param plugin Plugin to test
     * @param sandbox Sandbox to use
     * @return Validation result
     */
    ValidationResult testInSandbox(PluginEntity plugin, PluginSandbox sandbox);
}
