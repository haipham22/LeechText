package dev.haipham22.leechtext.ui

import androidx.compose.runtime.ProvidedValue

/**
 * Providers override locale cho Res.string (N5 i18n). JVM/Android: reflection shim
 * CMP internal API; iOS: rỗng — dùng locale hệ thống (CMP chưa có API public,
 * issue compose-multiplatform #4571).
 */
internal expect fun appLanguageProviders(language: String): Array<ProvidedValue<*>>
