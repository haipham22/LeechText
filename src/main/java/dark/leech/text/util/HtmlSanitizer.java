package dark.leech.text.util;

import java.util.regex.Pattern;

/**
 * Utility class for sanitizing HTML to ensure well-formed XML. Fixes common issues like unclosed
 * tags that cause EPUB validation errors.
 */
public class HtmlSanitizer {

    /**
     * Sanitize HTML to fix common XML validation issues. Ensures all inline tags are properly
     * closed within block elements.
     *
     * @param html The HTML content to sanitize
     * @return Sanitized HTML with proper tag closing
     */
    public static String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        // Fix unclosed inline tags before block tags close
        // Common inline tags that might be left unclosed
        html = fixUnclosedTags(html, "strong");
        html = fixUnclosedTags(html, "em");
        html = fixUnclosedTags(html, "b");
        html = fixUnclosedTags(html, "i");
        html = fixUnclosedTags(html, "span");
        html = fixUnclosedTags(html, "a");

        // Remove any orphaned closing tags without opening tags
        html = removeOrphanedClosingTags(html);

        return html;
    }

    /**
     * Fix unclosed tags by ensuring they're closed before their parent block closes. For example:
     *
     * <p><strong>text becomes
     *
     * <p><strong>text</strong>
     *
     * @param html The HTML content
     * @param tag The tag name to fix
     * @return Fixed HTML
     */
    private static String fixUnclosedTags(String html, String tag) {
        // Pattern: opening tag, content, closing block tag (p, div, li, etc.) without closing the
        // inline tag
        // This is a simplified fix - handles common cases
        Pattern pattern =
                Pattern.compile(
                        "<(" + tag + ")[^>]*>([^<]*?)</(p|div|li|h[1-6]|td)>",
                        Pattern.CASE_INSENSITIVE);

        String replacement = "<$1>$2</$1></$3>";
        return pattern.matcher(html).replaceAll(replacement);
    }

    /**
     * Remove orphaned closing tags that don't have corresponding opening tags. This is a basic
     * implementation - for production use, consider using a proper HTML parser.
     *
     * @param html The HTML content
     * @return HTML with orphaned closing tags removed
     */
    private static String removeOrphanedClosingTags(String html) {
        // Remove closing tags for inline elements if they appear without opening tags
        // This is a simplified approach - checks if opening tag exists in the same paragraph
        String[] inlineTags = {"strong", "em", "b", "i", "span", "a"};

        for (String tag : inlineTags) {
            // Look for orphaned closing tags at the start of blocks or paragraphs
            Pattern pattern =
                    Pattern.compile(
                            "<(p|div|li|h[1-6]|td)>\\s*</" + tag + ">", Pattern.CASE_INSENSITIVE);
            html = pattern.matcher(html).replaceAll("<$1>");
        }

        return html;
    }

    /**
     * Validate HTML by checking tag balance. This is a basic validation - returns true if opening
     * and closing tags match.
     *
     * @param html The HTML content to validate
     * @return true if tags appear balanced, false otherwise
     */
    public static boolean isValid(String html) {
        if (html == null || html.isEmpty()) {
            return true;
        }

        // Count opening and closing tags for common elements
        String[] tagsToCheck = {"strong", "em", "p", "div", "span", "a"};

        for (String tag : tagsToCheck) {
            long openingCount = countOccurrences(html, "<" + tag);
            long closingCount = countOccurrences(html, "</" + tag + ">");

            if (openingCount != closingCount) {
                return false;
            }
        }

        return true;
    }

    /** Count occurrences of a substring in a string. */
    private static long countOccurrences(String str, String sub) {
        if (str == null || sub == null || str.isEmpty() || sub.isEmpty()) {
            return 0;
        }

        int count = 0;
        int idx = 0;

        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }

        return count;
    }
}
