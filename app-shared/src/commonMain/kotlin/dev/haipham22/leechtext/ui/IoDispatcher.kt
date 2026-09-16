package dev.haipham22.leechtext.ui

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatcher cho IO blocking (đọc properties.json, Http...) — Dispatchers.IO
 * là JVM-only (internal trên native). iOS: Default (coroutine IO nhỏ, đủ).
 */
internal expect val IoDispatcher: CoroutineDispatcher
