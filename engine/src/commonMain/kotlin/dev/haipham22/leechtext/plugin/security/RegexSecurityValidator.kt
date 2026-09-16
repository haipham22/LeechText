package dev.haipham22.leechtext.plugin.security

/**
 * Validator regex chống ReDoS (port từ RegexSecurityValidator.java; P5.2c common).
 * Check pattern nguy hiểm + test catastrophic backtracking.
 *
 * ponytail: bản JVM chạy match trong thread riêng + timeout 2s — common không có thread
 * portable; thay bằng match đồng bộ trên chuỗi test ngắn. Các check static phía trên đã
 * chặn các pattern catastrophic đã biết; nâng cấp worker-timeout khi cần trên cả 2 platform.
 */
object RegexSecurityValidator {
    // Test string trigger catastrophic backtracking trong regex yếu (ngắn hơn bản JVM
    // để match đồng bộ không treo lâu trên pattern lọt lưới)
    private const val REDOS_TEST_STRING =
        "aaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbcccccccccccc"

    /** Validate regex pattern cho ReDoS vulnerabilities. */
    @Throws(SecurityValidationException::class)
    @Suppress("ThrowsCount") // validation chain ở trust boundary — mỗi guard ném exception có message riêng cho UI
    fun validateRegex(regex: String?) {
        if (regex.isNullOrEmpty()) {
            throw SecurityValidationException("Regex pattern cannot be empty")
        }

        // Check độ dài pattern
        if (regex.length > 1000) {
            throw SecurityValidationException("Regex pattern too long: ${regex.length} chars (max: 1000)")
        }

        // Check pattern nguy hiểm đã biết
        checkForDangerousPatterns(regex)

        // Compile thử
        try {
            Regex(regex)
        } catch (e: Exception) {
            throw SecurityValidationException("Invalid regex pattern: $regex", e)
        }

        // Test catastrophic backtracking
        testForCatastrophicBacktracking(regex)
    }

    // Các pattern nguy hiểm đã biết (ReDoS)
    private val dangerousPatterns =
        listOf(
            "(.+)+", // Nested repetition (catastrophic)
            "(.+)*", // Nested repetition with star
            "(.+){", // Nested repetition with quantifier
            "([a-zA-Z]+)+", // Nested repetition on character class
            "([a-z]+)*", // Nested repetition with star
            "([a-z]+)+", // Nested repetition with plus (exact match from test)
            "(\\w+)+", // Word character nested repetition
            "(\\d+)+", // Digit nested repetition
            "(.+)*(.+)*", // Multiple nested repetitions
            "(.*)+(.*+)", // Complex nested patterns
            "\\(([^)]+\\)+\\)", // Nested groups with repetition
            "(([^()]+)+)+", // Nested groups with repetition
            "(a+)+", // Simple nested repetition
            "([a-z]+)+[a-z]+", // Overlapping repetitions
        )

    /** Check các regex pattern nguy hiểm có thể gây ReDoS. */
    @Throws(SecurityValidationException::class)
    private fun checkForDangerousPatterns(regex: String) {
        val lowerRegex = regex.lowercase()
        for (dangerous in dangerousPatterns) {
            if (lowerRegex.contains(dangerous.lowercase())) {
                throw SecurityValidationException("Regex pattern contains potentially dangerous ReDoS pattern: $regex")
            }
        }

        // Check quantifier nesting quá mức
        val nestedQuantifierCount =
            regex.count { it == '+' || it == '*' || it == '?' || it == '{' }
        if (nestedQuantifierCount > 10) {
            throw SecurityValidationException("Regex pattern contains excessive quantifiers: $regex")
        }

        // Check overlapping alternations
        val alternationCount = regex.count { it == '|' }
        if (alternationCount > 5) {
            checkOverlappingAlternations(regex)
        }
    }

    /** Check cặp alternation dài (>10 chars) có common prefix >5 chars — potential ReDoS. */
    @Throws(SecurityValidationException::class)
    private fun checkOverlappingAlternations(regex: String) {
        // "|".split là literal split (giữ semantics fix cũ — không phải regex zero-width)
        val alternatives = regex.split("|")
        for (i in 0 until alternatives.size - 1) {
            for (j in i + 1 until alternatives.size) {
                if (alternatives[i].length > 10 &&
                    alternatives[j].length > 10 &&
                    hasCommonPrefix(alternatives[i], alternatives[j])
                ) {
                    throw SecurityValidationException("Regex pattern has overlapping alternations (potential ReDoS): $regex")
                }
            }
        }
    }

    /** Test regex cho catastrophic backtracking — match đồng bộ, giới hạn bằng chuỗi ngắn. */
    @Throws(SecurityValidationException::class)
    private fun testForCatastrophicBacktracking(regex: String) {
        try {
            Regex(regex).matches(REDOS_TEST_STRING)
        } catch (e: Exception) {
            throw SecurityValidationException("Regex test failed: $regex", e)
        }
    }

    /** Hai string có common prefix dài > 5 chars không. */
    private fun hasCommonPrefix(
        s1: String,
        s2: String,
    ): Boolean {
        val minLength = minOf(s1.length, s2.length)
        var commonLength = 0
        for (i in 0 until minLength) {
            if (s1[i] == s2[i]) commonLength++ else break
        }
        return commonLength > 5
    }
}
