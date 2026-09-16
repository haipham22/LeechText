package dev.haipham22.leechtext.util

/** Wall-clock millis (TTL cache, timestamp) — actual per-platform. */
expect fun nowMillis(): Long

/** Ngày hiện tại format dd-MM-yyyy (cho content.opf) — actual per-platform. */
expect fun formatDateDDMMYYYY(): String
