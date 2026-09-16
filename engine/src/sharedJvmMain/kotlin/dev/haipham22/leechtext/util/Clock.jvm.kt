package dev.haipham22.leechtext.util

/** JVM actual — System.currentTimeMillis. */
actual fun nowMillis(): Long = System.currentTimeMillis()

/** JVM actual — SimpleDateFormat dd-MM-yyyy. */
actual fun formatDateDDMMYYYY(): String = java.text.SimpleDateFormat("dd-MM-yyyy").format(java.util.Date())
