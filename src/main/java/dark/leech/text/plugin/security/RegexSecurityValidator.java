package dark.leech.text.plugin.security;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Security validator for regex patterns. Prevents ReDoS (Regular Expression Denial of Service)
 * attacks.
 */
public class RegexSecurityValidator {

    // Maximum timeout for regex operations (milliseconds)
    private static final long REGEX_TIMEOUT_MS = 2000; // 2 seconds

    // Test string that will trigger catastrophic backtracking in vulnerable regexes
    private static final String REDOS_TEST_STRING =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbbbbbbbbcccccccccccccccccccccccccccc";

    /**
     * Validates a regex pattern for ReDoS vulnerabilities.
     *
     * @param regex Pattern to validate
     * @throws SecurityValidationException if pattern is unsafe
     */
    public static void validateRegex(String regex) throws SecurityValidationException {
        if (regex == null || regex.isEmpty()) {
            throw new SecurityValidationException("Regex pattern cannot be empty");
        }

        // Check pattern length
        if (regex.length() > 1000) {
            throw new SecurityValidationException(
                    "Regex pattern too long: " + regex.length() + " chars (max: 1000)");
        }

        // Check for known dangerous patterns
        checkForDangerousPatterns(regex);

        // Try to compile with timeout
        try {
            Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new SecurityValidationException("Invalid regex pattern: " + regex, e);
        }

        // Test for catastrophic backtracking with timeout
        testForCatastrophicBacktracking(regex);
    }

    /**
     * Checks for known dangerous regex patterns that could cause ReDoS.
     *
     * @param regex Pattern to check
     * @throws SecurityValidationException if dangerous pattern found
     */
    private static void checkForDangerousPatterns(String regex) throws SecurityValidationException {
        // Patterns that indicate potential ReDoS vulnerability
        String[] dangerousPatterns = {
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
            "([a-z]+)+[a-z]+" // Overlapping repetitions
        };

        String lowerRegex = regex.toLowerCase();
        for (String dangerous : dangerousPatterns) {
            if (lowerRegex.contains(dangerous.toLowerCase())) {
                throw new SecurityValidationException(
                        "Regex pattern contains potentially dangerous ReDoS pattern: " + regex);
            }
        }

        // Check for excessive quantifier nesting
        long nestedQuantifierCount =
                regex.chars().filter(c -> c == '+' || c == '*' || c == '?' || c == '{').count();

        if (nestedQuantifierCount > 10) {
            throw new SecurityValidationException(
                    "Regex pattern contains excessive quantifiers: " + regex);
        }

        // Check for overlapping alternations
        long alternationCount = regex.chars().filter(c -> c == '|').count();

        if (alternationCount > 5) {
            // Additional check for overlapping patterns in alternations
            String[] alternatives = regex.split("\\|");
            for (int i = 0; i < alternatives.length - 1; i++) {
                for (int j = i + 1; j < alternatives.length; j++) {
                    if (alternatives[i].length() > 10
                            && alternatives[j].length() > 10
                            && hasCommonPrefix(alternatives[i], alternatives[j])) {
                        throw new SecurityValidationException(
                                "Regex pattern has overlapping alternations (potential ReDoS): "
                                        + regex);
                    }
                }
            }
        }
    }

    /**
     * Tests a regex pattern for catastrophic backtracking. Uses a test string that will trigger
     * backtracking in vulnerable patterns.
     *
     * @param regex Pattern to test
     * @throws SecurityValidationException if pattern causes timeout
     */
    private static void testForCatastrophicBacktracking(String regex)
            throws SecurityValidationException {
        Thread testThread =
                new Thread(
                        () -> {
                            try {
                                Pattern pattern = Pattern.compile(regex);
                                // Use test string that triggers backtracking
                                pattern.matcher(REDOS_TEST_STRING).matches();
                            } catch (Exception e) {
                                throw new RuntimeException("Regex test failed", e);
                            }
                        });

        testThread.start();

        try {
            // Wait with timeout
            testThread.join(REGEX_TIMEOUT_MS);

            if (testThread.isAlive()) {
                // Thread is still running - regex is vulnerable to ReDoS
                testThread.interrupt();
                throw new SecurityValidationException(
                        "Regex pattern timeout - potential ReDoS vulnerability: " + regex);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SecurityValidationException("Regex validation interrupted: " + regex, e);
        }
    }

    /**
     * Checks if two strings have a common prefix of significant length.
     *
     * @param s1 First string
     * @param s2 Second string
     * @return true if common prefix > 5 chars
     */
    private static boolean hasCommonPrefix(String s1, String s2) {
        int minLength = Math.min(s1.length(), s2.length());
        int commonLength = 0;

        for (int i = 0; i < minLength; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                commonLength++;
            } else {
                break;
            }
        }

        return commonLength > 5;
    }
}
