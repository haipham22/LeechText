package dark.leech.text.plugin.js.api;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;

import dark.leech.text.action.Log;

/**
 * Text utilities API for JavaScript plugins using Rhino. Provides string manipulation, encoding,
 * decoding, and text processing.
 */
public class Text extends JsApiWrapper {

    /**
     * Create Text API with execution context for proper object creation.
     *
     * @param context The Rhino Context for script execution
     * @param scope The Rhino Scriptable scope
     */
    public Text(Context context, Scriptable scope) {
        super(context, scope);
    }

    /** Create Text API without context (legacy compatibility). */
    public Text() {
        super();
    }

    /** Trim whitespace from both ends. Usage: text.trim(" hello ") => "hello" */
    public String trim(String text) {
        return text != null ? text.trim() : "";
    }

    /** Trim specified characters from start. */
    public String trimStart(String text, String chars) {
        if (text == null) {
            return "";
        }
        if (chars == null || chars.isEmpty()) {
            return text;
        }
        int start = 0;
        while (start < text.length() && chars.indexOf(text.charAt(start)) >= 0) {
            start++;
        }
        return text.substring(start);
    }

    /** Trim specified characters from end. */
    public String trimEnd(String text, String chars) {
        if (text == null) {
            return "";
        }
        if (chars == null || chars.isEmpty()) {
            return text;
        }
        int end = text.length();
        while (end > 0 && chars.indexOf(text.charAt(end - 1)) >= 0) {
            end--;
        }
        return text.substring(0, end);
    }

    /** Convert to lowercase. */
    public String lower(String text) {
        return text != null ? text.toLowerCase() : "";
    }

    /** Convert to uppercase. */
    public String upper(String text) {
        return text != null ? text.toUpperCase() : "";
    }

    /** Capitalize first letter. */
    public String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.substring(0, 1).toUpperCase() + (text.length() > 1 ? text.substring(1) : "");
    }

    /** Convert to title case (capitalize each word). */
    public String titleCase(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (result.length() > 0) {
                    result.append(" ");
                }
                result.append(word.substring(0, 1).toUpperCase());
                if (word.length() > 1) {
                    result.append(word.substring(1).toLowerCase());
                }
            }
        }
        return result.toString();
    }

    /** Split text by delimiter. */
    public NativeArray split(String text, String delimiter) {
        final String[] result;
        if (text == null) {
            result = new String[0];
        } else if (delimiter == null || delimiter.isEmpty()) {
            result = text.split("");
        } else {
            result = text.split(Pattern.quote(delimiter), -1);
        }

        Context ctx = getContext();
        Scriptable scope = getScope();
        return (NativeArray) ctx.newArray(scope, result);
    }

    /** Join array of strings with delimiter. */
    public String join(Object array, String delimiter) {
        if (array == null) {
            return "";
        }

        // Handle NativeArray
        if (array instanceof NativeArray) {
            NativeArray na = (NativeArray) array;
            StringBuilder result = new StringBuilder();
            String delim = delimiter != null ? delimiter : "";
            long size = na.getLength();

            for (long i = 0; i < size; i++) {
                Object item = na.get((int) i, na);
                if (i > 0) {
                    result.append(delim);
                }
                result.append(Context.toString(item));
            }
            return result.toString();
        }

        // Handle regular arrays
        if (array instanceof Object[]) {
            Object[] arr = (Object[]) array;
            StringBuilder result = new StringBuilder();
            String delim = delimiter != null ? delimiter : "";

            for (int i = 0; i < arr.length; i++) {
                if (i > 0) {
                    result.append(delim);
                }
                result.append(Context.toString(arr[i]));
            }
            return result.toString();
        }

        return "";
    }

    /** Replace all occurrences. */
    public String replace(String text, String target, String replacement) {
        if (text == null) {
            return "";
        }
        if (target == null || target.isEmpty()) {
            return text;
        }
        return text.replace(target, replacement != null ? replacement : "");
    }

    /** Replace first occurrence. */
    public String replaceFirst(String text, String target, String replacement) {
        if (text == null) {
            return "";
        }
        if (target == null || target.isEmpty()) {
            return text;
        }
        int index = text.indexOf(target);
        if (index >= 0) {
            return text.substring(0, index)
                    + (replacement != null ? replacement : "")
                    + text.substring(index + target.length());
        }
        return text;
    }

    /** Extract substring. */
    public String substring(String text, int start) {
        return substring(text, start, text != null ? text.length() : 0);
    }

    public String substring(String text, int start, int end) {
        if (text == null) {
            return "";
        }
        try {
            if (start < 0) {
                start = 0;
            }
            if (end < 0) {
                end = text.length() + end;
            }
            if (end > text.length()) {
                end = text.length();
            }
            return text.substring(start, end);
        } catch (IndexOutOfBoundsException e) {
            return "";
        }
    }

    /** Get text length. */
    public int length(String text) {
        return text != null ? text.length() : 0;
    }

    /** Check if text contains substring. */
    public boolean contains(String text, String substring) {
        if (text == null || substring == null) {
            return false;
        }
        return text.contains(substring);
    }

    /** Check if text starts with prefix. */
    public boolean startsWith(String text, String prefix) {
        if (text == null || prefix == null) {
            return false;
        }
        return text.startsWith(prefix);
    }

    /** Check if text ends with suffix. */
    public boolean endsWith(String text, String suffix) {
        if (text == null || suffix == null) {
            return false;
        }
        return text.endsWith(suffix);
    }

    /** Repeat text n times. */
    public String repeat(String text, int count) {
        if (text == null || count <= 0) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++) {
            result.append(text);
        }
        return result.toString();
    }

    /** Pad text on the left to reach length. */
    public String padLeft(String text, int length, String padChar) {
        if (text == null) {
            text = "";
        }
        if (padChar == null || padChar.isEmpty()) {
            padChar = " ";
        }
        while (text.length() < length) {
            text = padChar + text;
        }
        return text;
    }

    /** Pad text on the right to reach length. */
    public String padRight(String text, int length, String padChar) {
        if (text == null) {
            text = "";
        }
        if (padChar == null || padChar.isEmpty()) {
            padChar = " ";
        }
        while (text.length() < length) {
            text = text + padChar;
        }
        return text;
    }

    /** Extract text between start and end markers. */
    public String between(String text, String start, String end) {
        if (text == null || start == null || end == null) {
            return "";
        }
        int startIndex = text.indexOf(start);
        if (startIndex < 0) {
            return "";
        }
        startIndex += start.length();
        int endIndex = text.indexOf(end, startIndex);
        if (endIndex < 0) {
            return "";
        }
        return text.substring(startIndex, endIndex);
    }

    /** Extract all text between markers. */
    public NativeArray betweenAll(String text, String start, String end) {
        List<String> results = new ArrayList<>();
        if (text == null || start == null || end == null) {
            Context ctx = getContext();
            Scriptable scope = getScope();
            return (NativeArray) ctx.newArray(scope, new Object[0]);
        }

        int searchFrom = 0;
        while (true) {
            int startIndex = text.indexOf(start, searchFrom);
            if (startIndex < 0) break;
            startIndex += start.length();

            int endIndex = text.indexOf(end, startIndex);
            if (endIndex < 0) break;

            results.add(text.substring(startIndex, endIndex));
            searchFrom = endIndex + end.length();
        }

        Context ctx = getContext();
        Scriptable scope = getScope();
        return (NativeArray) ctx.newArray(scope, results.toArray());
    }

    /** Remove HTML tags from text. */
    public String stripHtml(String html) {
        if (html == null) {
            return "";
        }
        return html.replaceAll("<[^>]+>", "");
    }

    /** Extract first N characters. */
    public String truncate(String text, int maxLength) {
        return truncate(text, maxLength, "...");
    }

    public String truncate(String text, int maxLength, String suffix) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        String safeSuffix = suffix != null ? suffix : "";
        return text.substring(0, maxLength - safeSuffix.length()) + safeSuffix;
    }

    /** Reverse string. */
    public String reverse(String text) {
        if (text == null) {
            return "";
        }
        return new StringBuilder(text).reverse().toString();
    }

    /** Count occurrences of substring. */
    public int count(String text, String substring) {
        if (text == null || substring == null || substring.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) >= 0) {
            count++;
            index += substring.length();
        }
        return count;
    }

    /** Check if text matches regex pattern. */
    public boolean matches(String text, String pattern) {
        if (text == null || pattern == null) {
            return false;
        }
        try {
            return Pattern.matches(pattern, text);
        } catch (Exception e) {
            Log.add("Invalid regex pattern: " + pattern);
            return false;
        }
    }

    /** Extract all regex matches. */
    public NativeArray extractAll(String text, String pattern) {
        List<String> results = new ArrayList<>();
        if (text == null || pattern == null) {
            Context ctx = getContext();
            Scriptable scope = getScope();
            return (NativeArray) ctx.newArray(scope, new Object[0]);
        }
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            while (m.find()) {
                results.add(m.group());
            }
        } catch (Exception e) {
            Log.add("Invalid regex pattern: " + pattern);
        }
        Context ctx = getContext();
        Scriptable scope = getScope();
        return (NativeArray) ctx.newArray(scope, results.toArray());
    }

    /** Convert to byte array. */
    public byte[] toBytes(String text) {
        if (text == null) {
            return new byte[0];
        }
        return text.getBytes(StandardCharsets.UTF_8);
    }

    /** Convert from byte array. */
    public String fromBytes(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /** URL encode text. */
    public String urlEncode(String text) {
        if (text == null) {
            return "";
        }
        try {
            return URLEncoder.encode(text, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return text;
        }
    }

    /** URL decode text. */
    public String urlDecode(String text) {
        if (text == null) {
            return "";
        }
        try {
            return URLDecoder.decode(text, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return text;
        }
    }
}
