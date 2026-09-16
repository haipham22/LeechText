package dev.haipham22.leechtext.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Index JSON của một repo plugin (port từ models/Repository.java). */
@Serializable
data class Repository(
    @SerialName("metadata") var metaData: MetaData? = null,
    @SerialName("data") var plugins: List<Plugin>? = null,
) {
    @Serializable
    data class MetaData(
        var author: String? = null,
        var description: String? = null,
    )

    @Serializable
    data class Plugin(
        var name: String? = null,
        var uuid: String? = null,
        var author: String? = null,
        var path: String? = null,
        var version: Double = 0.0,
        var source: String? = null,
        var icon: String? = null,
        var description: String? = null,
        var type: String? = null,
        var locale: String? = null,
        var tag: String? = null,
    )
}
