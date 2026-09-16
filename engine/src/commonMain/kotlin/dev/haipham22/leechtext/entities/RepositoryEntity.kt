package dev.haipham22.leechtext.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Repo plugin đã lưu local (port từ enities/RepositoryEntity.java). Equal theo link. */
@Serializable
data class RepositoryEntity(
    var uuid: String? = null,
    var link: String? = null,
    var author: String? = null,
    var description: String? = null,
    @SerialName("enabled") var isEnabled: Boolean = false,
) {
    override fun equals(other: Any?): Boolean = other is RepositoryEntity && link == other.link

    override fun hashCode(): Int = link?.hashCode() ?: 0
}
