package dev.haipham22.leechtext.ui.settings

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** isNewer: so per-segment số học, strip prefix "v", tag rác → false. */
class UpdateCheckTest {
    @Test
    fun segmentNumericCompare() {
        assertTrue(UpdateCheck.isNewer("1.2.3", "v1.10.0")) // 10 > 2, không phải "10" < "2" kiểu chuỗi
    }

    @Test
    fun sameVersion() {
        assertFalse(UpdateCheck.isNewer("1.3.1", "v1.3.1"))
    }

    @Test
    fun olderMajor() {
        assertFalse(UpdateCheck.isNewer("2.0", "v1.9.9"))
    }

    @Test
    fun shorterTagPaddedWithZero() {
        assertTrue(UpdateCheck.isNewer("1.3.1", "v1.4")) // 1.4 == 1.4.0 > 1.3.1
        assertFalse(UpdateCheck.isNewer("1.4", "v1.4.0")) // pad 0 → bằng nhau
    }

    @Test
    fun garbageTag() {
        assertFalse(UpdateCheck.isNewer("1.3.1", "latest"))
        assertFalse(UpdateCheck.isNewer("1.3.1", "v1.x.2"))
        assertFalse(UpdateCheck.isNewer("", "v1.4.0"))
    }
}
