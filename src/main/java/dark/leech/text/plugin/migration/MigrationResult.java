package dark.leech.text.plugin.migration;

import java.util.ArrayList;
import java.util.List;

/** Result of plugin migration operation. Contains statistics and detailed results. */
public class MigrationResult {

    private final int totalPlugins;
    private final int successfulMigrations;
    private final int failedMigrations;
    private final List<String> errors;
    private final List<String> successes;
    private final long timestamp;

    private MigrationResult(Builder builder) {
        this.totalPlugins = builder.totalPlugins;
        this.successfulMigrations = builder.successfulMigrations;
        this.failedMigrations = builder.failedMigrations;
        this.errors = builder.errors != null ? builder.errors : List.of();
        this.successes = builder.successes != null ? builder.successes : List.of();
        this.timestamp = System.currentTimeMillis();
    }

    public int getTotalPlugins() {
        return totalPlugins;
    }

    public int getSuccessfulMigrations() {
        return successfulMigrations;
    }

    public int getFailedMigrations() {
        return failedMigrations;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getSuccesses() {
        return successes;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean isCompleteSuccess() {
        return failedMigrations == 0 && totalPlugins > 0;
    }

    public double getSuccessRate() {
        if (totalPlugins == 0) return 0.0;
        return (double) successfulMigrations / totalPlugins;
    }

    @Override
    public String toString() {
        return "MigrationResult{"
                + "totalPlugins="
                + totalPlugins
                + ", successfulMigrations="
                + successfulMigrations
                + ", failedMigrations="
                + failedMigrations
                + ", successRate="
                + String.format("%.1f%%", getSuccessRate() * 100)
                + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int totalPlugins = 0;
        private int successfulMigrations = 0;
        private int failedMigrations = 0;
        private List<String> errors = new ArrayList<>();
        private List<String> successes = new ArrayList<>();

        public Builder totalPlugins(int count) {
            this.totalPlugins = count;
            return this;
        }

        public Builder addSuccess(String pluginName) {
            this.successes.add(pluginName);
            this.successfulMigrations++;
            return this;
        }

        public Builder addError(String error) {
            this.errors.add(error);
            this.failedMigrations++;
            return this;
        }

        public MigrationResult build() {
            return new MigrationResult(this);
        }
    }
}
