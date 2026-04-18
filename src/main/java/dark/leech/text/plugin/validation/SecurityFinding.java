package dark.leech.text.plugin.validation;

/** Represents a security finding found during plugin scanning. */
public class SecurityFinding {

    private final String description;
    private final Severity severity;
    private final Category category;
    private final int lineNumber;

    public SecurityFinding(String description, Severity severity, Category category) {
        this(description, severity, category, -1);
    }

    public SecurityFinding(
            String description, Severity severity, Category category, int lineNumber) {
        this.description = description;
        this.severity = severity;
        this.category = category;
        this.lineNumber = lineNumber;
    }

    public String getDescription() {
        return description;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Category getCategory() {
        return category;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean hasLineNumber() {
        return lineNumber >= 0;
    }

    @Override
    public String toString() {
        if (hasLineNumber()) {
            return "SecurityFinding{"
                    + "severity="
                    + severity
                    + ", category="
                    + category
                    + ", line="
                    + lineNumber
                    + ", description='"
                    + description
                    + '\''
                    + '}';
        }
        return "SecurityFinding{"
                + "severity="
                + severity
                + ", category="
                + category
                + ", description='"
                + description
                + '\''
                + '}';
    }

    public enum Severity {
        /** Critical security issue - plugin must be rejected */
        CRITICAL,

        /** High severity issue - plugin should be rejected */
        HIGH,

        /** Medium severity issue - plugin should be reviewed */
        MEDIUM,

        /** Low severity issue - informational */
        LOW,

        /** Info only - no action required */
        INFO
    }

    public enum Category {
        /** Malicious code detected */
        MALWARE,

        /** Suspicious code pattern */
        SUSPICIOUS,

        /** Dangerous API usage */
        DANGEROUS_API,

        /** Code quality issue */
        CODE_QUALITY,

        /** Performance concern */
        PERFORMANCE,

        /** Security best practice violation */
        BEST_PRACTICE,

        /** Other category */
        OTHER
    }
}
