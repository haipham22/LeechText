package dev.haipham22.leechtext.models

import kotlinx.serialization.Serializable

/** Rule regex thay thế text (port từ models/Trash.java). */
@Serializable
data class Trash(
    var src: String? = "",
    var to: String? = "",
    var tip: String? = "",
    var replace: Boolean = true,
)
