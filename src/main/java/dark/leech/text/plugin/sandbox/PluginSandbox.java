package dark.leech.text.plugin.sandbox;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.api.ValidationResult;

/** Secure sandbox for plugin script execution. Enforces resource limits and API restrictions. */
public interface PluginSandbox {

    /**
     * Execute plugin script with resource limits.
     *
     * @param plugin Plugin to execute
     * @param script Script content to execute
     * @param context Execution context (APIs, variables)
     * @param <T> Return type
     * @return Future with execution result
     */
    <T> CompletableFuture<T> execute(
            PluginEntity plugin, String script, Map<String, Object> context);

    /**
     * Set resource limits for execution.
     *
     * @param maxMemoryBytes Maximum memory in bytes
     * @param maxCpuTimeMs Maximum CPU time in milliseconds
     * @param maxNetworkConnections Maximum network connections (0 = none)
     */
    void setLimits(long maxMemoryBytes, long maxCpuTimeMs, int maxNetworkConnections);

    /**
     * Check if plugin is allowed to use specific API.
     *
     * @param apiName API name (e.g., "Http.get", "File.write")
     * @return true if allowed
     */
    boolean isApiAllowed(String apiName);

    /** Get current resource limits. */
    ResourceLimits getLimits();

    /**
     * Test plugin without side effects. Validates script syntax and API usage.
     *
     * @param plugin Plugin to test
     * @return Validation result
     */
    ValidationResult test(PluginEntity plugin);

    /**
     * Check if sandbox is enabled.
     *
     * @return true if sandboxing is active
     */
    boolean isEnabled();

    /**
     * Enable or disable sandboxing. Warning: Disabling sandboxing is a security risk.
     *
     * @param enabled true to enable sandboxing
     */
    void setEnabled(boolean enabled);
}
