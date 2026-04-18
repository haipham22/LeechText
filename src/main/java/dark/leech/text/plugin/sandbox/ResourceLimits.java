package dark.leech.text.plugin.sandbox;

/** Resource limits for plugin execution. Used to prevent plugin resource exhaustion attacks. */
public class ResourceLimits {

    private final long maxMemoryBytes;
    private final long maxCpuTimeMs;
    private final int maxNetworkConnections;

    public ResourceLimits(long maxMemoryBytes, long maxCpuTimeMs, int maxNetworkConnections) {
        this.maxMemoryBytes = maxMemoryBytes;
        this.maxCpuTimeMs = maxCpuTimeMs;
        this.maxNetworkConnections = maxNetworkConnections;
    }

    /** Default resource limits for safe plugin execution. */
    public static ResourceLimits defaults() {
        return new ResourceLimits(
                100_000_000, // 100MB memory
                5_000, // 5 seconds CPU
                0 // No network by default
                );
    }

    /** Strict resource limits for untrusted plugins. */
    public static ResourceLimits strict() {
        return new ResourceLimits(
                50_000_000, // 50MB memory
                3_000, // 3 seconds CPU
                0 // No network
                );
    }

    /** Lenient resource limits for trusted plugins. */
    public static ResourceLimits lenient() {
        return new ResourceLimits(
                200_000_000, // 200MB memory
                10_000, // 10 seconds CPU
                5 // Allow 5 network connections
                );
    }

    /** Maximum resource limits (upper bound). */
    public static ResourceLimits maximum() {
        return new ResourceLimits(
                500_000_000, // 500MB memory
                30_000, // 30 seconds CPU
                10 // Max 10 network connections
                );
    }

    public long maxMemoryBytes() {
        return maxMemoryBytes;
    }

    public long maxCpuTimeMs() {
        return maxCpuTimeMs;
    }

    public int maxNetworkConnections() {
        return maxNetworkConnections;
    }

    public Builder toBuilder() {
        return new Builder()
                .maxMemoryBytes(maxMemoryBytes)
                .maxCpuTimeMs(maxCpuTimeMs)
                .maxNetworkConnections(maxNetworkConnections);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ResourceLimits{"
                + "maxMemoryBytes="
                + maxMemoryBytes
                + ", maxCpuTimeMs="
                + maxCpuTimeMs
                + ", maxNetworkConnections="
                + maxNetworkConnections
                + '}';
    }

    public static class Builder {
        private long maxMemoryBytes = 100_000_000;
        private long maxCpuTimeMs = 5_000;
        private int maxNetworkConnections = 0;

        public Builder maxMemoryBytes(long maxMemoryBytes) {
            this.maxMemoryBytes = maxMemoryBytes;
            return this;
        }

        public Builder maxCpuTimeMs(long maxCpuTimeMs) {
            this.maxCpuTimeMs = maxCpuTimeMs;
            return this;
        }

        public Builder maxNetworkConnections(int maxNetworkConnections) {
            this.maxNetworkConnections = maxNetworkConnections;
            return this;
        }

        public ResourceLimits build() {
            return new ResourceLimits(maxMemoryBytes, maxCpuTimeMs, maxNetworkConnections);
        }
    }
}
