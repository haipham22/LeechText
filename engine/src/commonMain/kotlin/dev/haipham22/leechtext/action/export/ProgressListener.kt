package dev.haipham22.leechtext.action.export

/** Callback tiến độ export (port từ listeners/ProgressListener.java — không Swing). */
fun interface ProgressListener {
    fun setProgress(
        value: Int,
        string: String,
    )
}
