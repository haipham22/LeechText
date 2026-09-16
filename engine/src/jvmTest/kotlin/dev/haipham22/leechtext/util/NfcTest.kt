package dev.haipham22.leechtext.util

import kotlin.test.Test
import kotlin.test.assertEquals

class NfcTest {
    @Test
    fun nfdComposesToNfc() {
        val nfd = "Tie\u0302\u0301ng Vie\u0323\u0302t \u2014 ch\u01B0\u01A1ng ha\u0302\u0323ng" // decomposed
        assertEquals("Tiếng Việt — chương h" + 'ậ' + "ng", nfd.nfc())
    }
}
