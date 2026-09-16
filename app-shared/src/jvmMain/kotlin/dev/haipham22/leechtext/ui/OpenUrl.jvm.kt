package dev.haipham22.leechtext.ui

import java.net.URI

/** Desktop: browser mặc định qua java.awt.Desktop. */
actual fun openUrl(url: String) {
    runCatching { java.awt.Desktop.getDesktop().browse(URI(url)) }
}
