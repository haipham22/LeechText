package dev.haipham22.leechtext.plugin.security

import kotlin.test.Test
import kotlin.test.assertFailsWith

class RegexSecurityValidatorTest {
    @Test
    fun validRegexPasses() {
        RegexSecurityValidator.validateRegex("[a-z]+\\.com") // không throw
        RegexSecurityValidator.validateRegex("^https?://example\\.com/.*$")
        RegexSecurityValidator.validateRegex("truyen-(cu|moi)\\.html")
    }

    @Test
    fun emptyOrNullRegexBlocked() {
        assertFailsWith<SecurityValidationException> { RegexSecurityValidator.validateRegex("") }
        assertFailsWith<SecurityValidationException> { RegexSecurityValidator.validateRegex(null) }
    }

    @Test
    fun regexTooLongBlocked() {
        assertFailsWith<SecurityValidationException> {
            RegexSecurityValidator.validateRegex("a".repeat(1001))
        }
    }

    @Test
    fun nestedRepetitionReDoSPatternBlocked() {
        listOf("(a+)+", "(.+)*", "(.+)+", "(\\d+)+", "(\\w+)+", "([a-z]+)+", "([a-zA-Z]+)+")
            .forEach {
                assertFailsWith<SecurityValidationException>("phải chặn ReDoS pattern $it") {
                    RegexSecurityValidator.validateRegex(it)
                }
            }
    }

    @Test
    fun invalidRegexSyntaxBlocked() {
        assertFailsWith<SecurityValidationException> { RegexSecurityValidator.validateRegex("[") }
        assertFailsWith<SecurityValidationException> { RegexSecurityValidator.validateRegex("(unclosed") }
    }

    @Test
    fun tooManyQuantifiersBlocked() {
        // 11 dấu '+' → vượt limit 10
        assertFailsWith<SecurityValidationException> {
            RegexSecurityValidator.validateRegex("a+a+a+a+a+a+a+a+a+a+a+")
        }
    }

    @Test
    fun longOverlappingAlternationWithCommonPrefixBlocked() {
        val regex =
            "https://aaaa|https://bbbb|https://cccc|https://dddd|https://eeee|https://ffff|https://gggg"
        assertFailsWith<SecurityValidationException> {
            RegexSecurityValidator.validateRegex(regex)
        }
    }
}
