package dark.leech.text.plugin.sandbox;

/** Exception thrown when sandbox security is violated. */
public class SandboxSecurityException extends RuntimeException {

    private final ViolationType violationType;

    public enum ViolationType {
        /** Script execution timeout */
        TIMEOUT,

        /** Memory limit exceeded */
        MEMORY_LIMIT_EXCEEDED,

        /** CPU time limit exceeded */
        CPU_LIMIT_EXCEEDED,

        /** Network access blocked */
        NETWORK_ACCESS_BLOCKED,

        /** File access blocked */
        FILE_ACCESS_BLOCKED,

        /** Dangerous API access */
        DANGEROUS_API_ACCESS,

        /** Process creation blocked */
        PROCESS_CREATION_BLOCKED,

        /** Thread creation blocked */
        THREAD_CREATION_BLOCKED,

        /** Native access blocked */
        NATIVE_ACCESS_BLOCKED,

        /** Unknown security violation */
        UNKNOWN
    }

    public SandboxSecurityException(String message) {
        super(message);
        this.violationType = ViolationType.UNKNOWN;
    }

    public SandboxSecurityException(String message, Throwable cause) {
        super(message, cause);
        this.violationType = ViolationType.UNKNOWN;
    }

    public SandboxSecurityException(String message, ViolationType violationType) {
        super(message);
        this.violationType = violationType;
    }

    public SandboxSecurityException(String message, ViolationType violationType, Throwable cause) {
        super(message, cause);
        this.violationType = violationType;
    }

    public ViolationType getViolationType() {
        return violationType;
    }
}
