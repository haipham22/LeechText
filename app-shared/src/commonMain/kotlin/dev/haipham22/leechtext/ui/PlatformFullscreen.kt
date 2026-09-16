package dev.haipham22.leechtext.ui

/**
 * Vào/ra fullscreen thật của OS (không phải chỉ ẩn chrome in-app):
 * desktop — AWT Frame fullscreen; Android — immersive system bars.
 */
expect fun togglePlatformFullscreen()
