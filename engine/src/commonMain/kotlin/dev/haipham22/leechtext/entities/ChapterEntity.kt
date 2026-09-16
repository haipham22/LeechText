package dev.haipham22.leechtext.entities

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Entry mục lục từ plugin (port từ enities/ChapterEntity.java). */
@Serializable
data class ChapterEntity(
    @SerialName("id") var id: Int = 0,
    @SerialName("chapter_name") var name: String? = null,
    @SerialName("chapter_url") var url: String? = null,
)
