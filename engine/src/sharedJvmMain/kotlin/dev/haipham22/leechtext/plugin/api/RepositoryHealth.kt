package dev.haipham22.leechtext.plugin.api

/**
 * Health status của repository source (port từ api/RepositoryHealth.java) — dùng cho
 * monitoring/circuit breaking.
 */
data class RepositoryHealth(
    val status: Status = Status.UNKNOWN,
    val message: String = "",
    val lastCheckTime: Long = System.currentTimeMillis(),
    val failureCount: Int = 0,
    val lastSuccessTime: Long = 0,
) {
    enum class Status { HEALTHY, DEGRADED, UNHEALTHY, UNKNOWN }

    fun isHealthy(): Boolean = status == Status.HEALTHY

    companion object {
        @JvmStatic
        fun healthy(): RepositoryHealth = RepositoryHealth(
            status = Status.HEALTHY,
            message = "Repository is healthy",
            failureCount = 0,
            lastSuccessTime = System.currentTimeMillis(),
        )

        @JvmStatic
        fun unhealthy(reason: String?): RepositoryHealth = RepositoryHealth(
            status = Status.UNHEALTHY,
            message = reason ?: "",
            failureCount = 1,
            lastSuccessTime = 0,
        )

        @JvmStatic
        fun unknown(): RepositoryHealth = RepositoryHealth(
            status = Status.UNKNOWN,
            message = "Health status unknown",
            failureCount = 0,
            lastSuccessTime = 0,
        )
    }
}

/**
 * Metadata repository (port từ api/RepositoryMetadata.java) — version, update check URL, etc.
 */
data class RepositoryMetadata(
    val name: String = "",
    val description: String = "",
    val version: String = "1.0.0",
    val updateCheckUrl: String = "",
    val homepageUrl: String = "",
    val author: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val pluginCount: Int = 0,
)
