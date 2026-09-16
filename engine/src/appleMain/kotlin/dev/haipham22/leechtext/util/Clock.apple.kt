package dev.haipham22.leechtext.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.timeIntervalSince1970

/** Apple actual — NSDate epoch millis. */
@OptIn(ExperimentalForeignApi::class)
actual fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

/** Apple actual — NSDateFormatter dd-MM-yyyy. */
actual fun formatDateDDMMYYYY(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "dd-MM-yyyy"
    return formatter.stringFromDate(NSDate())
}
