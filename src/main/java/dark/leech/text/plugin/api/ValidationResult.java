package dark.leech.text.plugin.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Result of plugin validation. Contains validation status, errors, and warnings. */
public class ValidationResult {

    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;

    private ValidationResult(Builder builder) {
        this.valid = builder.valid;
        this.errors = builder.errors != null ? builder.errors : List.of();
        this.warnings = builder.warnings != null ? builder.warnings : List.of();
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public static ValidationResult success() {
        return new Builder().valid(true).errors(List.of()).warnings(List.of()).build();
    }

    public static ValidationResult success(List<String> warnings) {
        return new Builder().valid(true).errors(List.of()).warnings(warnings).build();
    }

    public static ValidationResult failure(String error) {
        return new Builder().valid(false).errors(Arrays.asList(error)).warnings(List.of()).build();
    }

    public static ValidationResult failure(List<String> errors) {
        return new Builder().valid(false).errors(errors).warnings(List.of()).build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean valid = true;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public Builder valid(boolean valid) {
            this.valid = valid;
            return this;
        }

        public Builder addError(String error) {
            this.errors.add(error);
            this.valid = false;
            return this;
        }

        public Builder addWarning(String warning) {
            this.warnings.add(warning);
            return this;
        }

        public Builder errors(List<String> errors) {
            this.errors = errors;
            if (errors != null && !errors.isEmpty()) {
                this.valid = false;
            }
            return this;
        }

        public Builder warnings(List<String> warnings) {
            this.warnings = warnings;
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(this);
        }
    }

    @Override
    public String toString() {
        return "ValidationResult{"
                + "valid="
                + valid
                + ", errors="
                + errors
                + ", warnings="
                + warnings
                + '}';
    }
}
