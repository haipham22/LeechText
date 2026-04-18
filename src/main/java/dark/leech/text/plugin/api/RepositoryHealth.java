package dark.leech.text.plugin.api;

/** Health status of a repository source. Used for monitoring and circuit breaking. */
public class RepositoryHealth {

    private final Status status;
    private final String message;
    private final long lastCheckTime;
    private final int failureCount;
    private final long lastSuccessTime;

    private RepositoryHealth(Builder builder) {
        this.status = builder.status;
        this.message = builder.message;
        this.lastCheckTime = builder.lastCheckTime;
        this.failureCount = builder.failureCount;
        this.lastSuccessTime = builder.lastSuccessTime;
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public long getLastCheckTime() {
        return lastCheckTime;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public long getLastSuccessTime() {
        return lastSuccessTime;
    }

    public boolean isHealthy() {
        return status == Status.HEALTHY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static RepositoryHealth healthy() {
        return new Builder()
                .status(Status.HEALTHY)
                .message("Repository is healthy")
                .lastCheckTime(System.currentTimeMillis())
                .failureCount(0)
                .lastSuccessTime(System.currentTimeMillis())
                .build();
    }

    public static RepositoryHealth unhealthy(String reason) {
        return new Builder()
                .status(Status.UNHEALTHY)
                .message(reason)
                .lastCheckTime(System.currentTimeMillis())
                .failureCount(1)
                .lastSuccessTime(0)
                .build();
    }

    public static RepositoryHealth unknown() {
        return new Builder()
                .status(Status.UNKNOWN)
                .message("Health status unknown")
                .lastCheckTime(System.currentTimeMillis())
                .failureCount(0)
                .lastSuccessTime(0)
                .build();
    }

    public static class Builder {
        private Status status = Status.UNKNOWN;
        private String message = "";
        private long lastCheckTime = System.currentTimeMillis();
        private int failureCount = 0;
        private long lastSuccessTime = 0;

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder lastCheckTime(long lastCheckTime) {
            this.lastCheckTime = lastCheckTime;
            return this;
        }

        public Builder failureCount(int failureCount) {
            this.failureCount = failureCount;
            return this;
        }

        public Builder lastSuccessTime(long lastSuccessTime) {
            this.lastSuccessTime = lastSuccessTime;
            return this;
        }

        public RepositoryHealth build() {
            return new RepositoryHealth(this);
        }
    }

    public enum Status {
        /** Repository is responding correctly */
        HEALTHY,

        /** Repository is having issues but may recover */
        DEGRADED,

        /** Repository is not responding or has critical errors */
        UNHEALTHY,

        /** Health status has not been checked yet */
        UNKNOWN
    }
}
