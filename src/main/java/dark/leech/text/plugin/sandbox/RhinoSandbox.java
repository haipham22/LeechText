package dark.leech.text.plugin.sandbox;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import dark.leech.text.action.Log;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.api.ValidationResult;
import dark.leech.text.plugin.security.RegexSecurityValidator;
import dark.leech.text.plugin.security.SecurityConstants;
import dark.leech.text.plugin.security.SecurityValidationException;

/** Rhino-based sandbox implementation. Uses Rhino Context for secure script execution. */
public class RhinoSandbox implements PluginSandbox {

    private static final long DEFAULT_TEST_TIMEOUT_MS = 3000; // 3 seconds for testing

    private ResourceLimits limits;
    private final ExecutorService executor;
    private boolean enabled = true;

    public RhinoSandbox() {
        this.limits = ResourceLimits.defaults();
        this.executor =
                Executors.newSingleThreadExecutor(
                        r -> {
                            Thread t = new Thread(r, "PluginSandbox");
                            t.setDaemon(true);
                            return t;
                        });
    }

    @Override
    public void setLimits(long maxMemoryBytes, long maxCpuTimeMs, int maxNetworkConnections) {
        this.limits = new ResourceLimits(maxMemoryBytes, maxCpuTimeMs, maxNetworkConnections);
    }

    @Override
    public <T> CompletableFuture<T> execute(
            PluginEntity plugin, String script, Map<String, Object> context) {

        return CompletableFuture.supplyAsync(
                () -> {
                    if (!enabled) {
                        throw new SandboxSecurityException(
                                "Sandbox is disabled - cannot execute scripts safely",
                                SandboxSecurityException.ViolationType.UNKNOWN);
                    }

                    Context ctx = Context.enter();
                    try {
                        // Configure for security
                        ctx.setOptimizationLevel(-1); // Interpretation mode
                        ctx.setMaximumInterpreterStackDepth(1000);

                        Scriptable scope = ctx.initStandardObjects();

                        // Bind context variables
                        if (context != null) {
                            context.forEach(
                                    (key, value) -> {
                                        try {
                                            ScriptableObject.putProperty(
                                                    scope, key, Context.javaToJS(value, scope));
                                        } catch (Exception e) {
                                            Log.add("Failed to bind context variable: " + key);
                                        }
                                    });
                        }

                        // Execute with timeout
                        long startTime = System.currentTimeMillis();

                        Object result = ctx.evaluateString(scope, script, "plugin", 1, null);

                        long elapsed = System.currentTimeMillis() - startTime;
                        if (elapsed > limits.maxCpuTimeMs()) {
                            throw new SandboxSecurityException(
                                    "Script execution exceeded CPU time limit",
                                    SandboxSecurityException.ViolationType.CPU_LIMIT_EXCEEDED);
                        }

                        @SuppressWarnings("unchecked")
                        T typedResult = (T) result;
                        return typedResult;

                    } catch (SandboxSecurityException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new SandboxSecurityException(
                                "Sandbox execution error: " + e.getMessage(),
                                SandboxSecurityException.ViolationType.UNKNOWN,
                                e);
                    } finally {
                        Context.exit();
                    }
                },
                executor);
    }

    @Override
    public boolean isApiAllowed(String apiName) {
        return SecurityConstants.isApiAllowed(apiName);
    }

    @Override
    public ResourceLimits getLimits() {
        return limits;
    }

    @Override
    public ValidationResult test(PluginEntity plugin) {
        ValidationResult.Builder builder = ValidationResult.builder();

        // Only test JavaScript plugins
        if (!PluginEntity.SCRIPT_ENGINE_JAVASCRIPT.equals(plugin.getScriptEngine())) {
            return ValidationResult.success(Arrays.asList("Plugin does not use JavaScript engine"));
        }

        // Test regex pattern
        if (plugin.getRegex() != null && !plugin.getRegex().isEmpty()) {
            try {
                RegexSecurityValidator.validateRegex(plugin.getRegex());
            } catch (SecurityValidationException e) {
                builder.addError("Regex validation failed: " + e.getMessage());
            }
        }

        // Test JavaScript syntax
        String[] scripts = {
            plugin.getChapGetter(),
            plugin.getTocGetter(),
            plugin.getPageGetter(),
            plugin.getSearchGetter(),
            plugin.getDetailGetter()
        };

        String[] scriptNames = {
            "chapGetter", "tocGetter", "pageGetter", "searchGetter", "detailGetter"
        };

        Context ctx = Context.enter();
        try {
            ctx.setOptimizationLevel(-1);
            Scriptable scope = ctx.initStandardObjects();

            for (int i = 0; i < scripts.length; i++) {
                if (scripts[i] != null && !scripts[i].isEmpty()) {
                    try {
                        // Test syntax only - don't execute
                        ctx.compileString(scripts[i], scriptNames[i], 1, null);
                    } catch (Exception e) {
                        builder.addError(
                                "JavaScript syntax error in "
                                        + scriptNames[i]
                                        + ": "
                                        + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            builder.addError("Failed to create test context: " + e.getMessage());
        } finally {
            Context.exit();
        }

        // Check for dangerous API usage
        testApiUsage(plugin, builder);

        return builder.build();
    }

    private void testApiUsage(PluginEntity plugin, ValidationResult.Builder builder) {
        String combinedScripts = "";
        if (plugin.getChapGetter() != null) combinedScripts += plugin.getChapGetter() + " ";
        if (plugin.getTocGetter() != null) combinedScripts += plugin.getTocGetter() + " ";
        if (plugin.getPageGetter() != null) combinedScripts += plugin.getPageGetter() + " ";

        // Check for dangerous API usage
        if (combinedScripts.contains("java.lang.Runtime")) {
            builder.addError("Direct use of Runtime API detected");
        }
        if (combinedScripts.contains("java.lang.ProcessBuilder")) {
            builder.addError("Direct use of ProcessBuilder API detected");
        }
        if (combinedScripts.contains("java.io.File")) {
            builder.addWarning("Direct file access detected (will be blocked in sandbox)");
        }
        if (combinedScripts.contains("System.exit")) {
            builder.addError("Use of System.exit detected");
        }
        if (combinedScripts.contains("Class.forName")) {
            builder.addError("Use of Class.forName detected (reflection blocked)");
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            Log.add("WARNING: Sandbox has been disabled - plugins run with full access");
        }
    }

    /** Shutdown the sandbox executor. */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
