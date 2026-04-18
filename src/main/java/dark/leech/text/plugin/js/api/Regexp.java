package dark.leech.text.plugin.js.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import dark.leech.text.action.Log;

/**
 * Regular Expression API for JavaScript plugins using Rhino. Provides pattern matching, finding,
 * replacing, and splitting.
 */
public class Regexp extends JsApiWrapper {

    /**
     * Create Regexp API with execution context for proper object creation.
     *
     * @param context The Rhino Context for script execution
     * @param scope The Rhino Scriptable scope
     */
    public Regexp(Context context, Scriptable scope) {
        super(context, scope);
    }

    /** Create Regexp API without context (legacy compatibility). */
    public Regexp() {
        super();
    }

    /** Find first match in text. Usage: regexp.find("Hello World", "llo") => "llo" */
    public String find(String text, String pattern) {
        if (text == null || pattern == null) {
            return "";
        }
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.group();
            }
            return "";
        } catch (PatternSyntaxException e) {
            Log.add("Invalid regex pattern: " + pattern);
            return "";
        }
    }

    /** Find all matches in text. Usage: regexp.findAll("Hello World", "l") => ["l", "l", "l"] */
    public Object[] findAll(String text, String pattern) {
        List<String> results = new ArrayList<>();
        if (text != null && pattern != null) {
            try {
                Pattern p = Pattern.compile(pattern);
                Matcher m = p.matcher(text);
                while (m.find()) {
                    results.add(m.group());
                }
            } catch (PatternSyntaxException e) {
                Log.add("Invalid regex pattern: " + pattern);
            }
        }
        return results.toArray();
    }

    /** Match all with groups and indices. Usage: regexp.matchAll("test123", "\\d+") */
    public Object[] matchAll(String text, String pattern) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (text == null || pattern == null) {
            return new Object[0];
        }

        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);

            int matchCount = 0;
            while (m.find()) {
                Map<String, Object> matchInfo = new HashMap<>();
                matchInfo.put("match", m.group());
                matchInfo.put("start", m.start());
                matchInfo.put("end", m.end());
                matchInfo.put("index", matchCount++);

                // Add groups
                int groupCount = m.groupCount();
                if (groupCount > 0) {
                    List<String> groups = new ArrayList<>();
                    for (int i = 1; i <= groupCount; i++) {
                        groups.add(m.group(i));
                    }
                    matchInfo.put("groups", groups.toArray());
                }

                results.add(matchInfo);
            }
        } catch (PatternSyntaxException e) {
            Log.add("Invalid regex pattern: " + pattern);
        }

        return results.toArray();
    }

    /** Replace first occurrence. */
    public String replace(String text, String pattern, String replacement) {
        if (text == null || pattern == null) {
            return text != null ? text : "";
        }
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            if (m.find()) {
                return m.replaceFirst(replacement != null ? replacement : "");
            }
            return text;
        } catch (PatternSyntaxException e) {
            Log.add("Invalid regex pattern: " + pattern);
            return text;
        }
    }

    /** Replace all occurrences. */
    public String replaceAll(String text, String pattern, String replacement) {
        if (text == null || pattern == null) {
            return text != null ? text : "";
        }
        try {
            Pattern p = Pattern.compile(pattern);
            return p.matcher(text).replaceAll(replacement != null ? replacement : "");
        } catch (PatternSyntaxException e) {
            Log.add("Invalid regex pattern: " + pattern);
            return text;
        }
    }

    /** Split text by pattern. */
    public Object[] split(String text, String pattern) {
        if (text == null || pattern == null) {
            return text != null ? new Object[] {text} : new Object[0];
        }
        try {
            return text.split(pattern, -1);
        } catch (PatternSyntaxException e) {
            Log.add("Invalid regex pattern: " + pattern);
            return new Object[] {text};
        }
    }

    /** Test if pattern matches. */
    public boolean test(String text, String pattern) {
        if (text == null || pattern == null) {
            return false;
        }
        try {
            Pattern p = Pattern.compile(pattern);
            return p.matcher(text).find();
        } catch (PatternSyntaxException e) {
            Log.add("Invalid regex pattern: " + pattern);
            return false;
        }
    }

    /** Escape regex special characters. */
    public String escape(String text) {
        if (text == null) {
            return "";
        }
        return Pattern.quote(text);
    }

    /** Find last match. */
    public String findLast(String text, String pattern) {
        if (text == null || pattern == null) {
            return "";
        }
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            String lastMatch = "";
            while (m.find()) {
                lastMatch = m.group();
            }
            return lastMatch;
        } catch (PatternSyntaxException e) {
            Log.add("Invalid regex pattern: " + pattern);
            return "";
        }
    }

    /** Count occurrences. */
    public int count(String text, String pattern) {
        if (text == null || pattern == null) {
            return 0;
        }
        int count = 0;
        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);
            while (m.find()) {
                count++;
            }
        } catch (PatternSyntaxException e) {
            Log.add("Invalid regex pattern: " + pattern);
        }
        return count;
    }

    /** Get all groups from matches. */
    public Object[] groups(String text, String pattern) {
        List<String> groupList = new ArrayList<>();
        if (text == null || pattern == null) {
            return new Object[0];
        }

        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);

            if (m.find()) {
                int groupCount = m.groupCount();
                for (int i = 0; i <= groupCount; i++) {
                    groupList.add(m.group(i));
                }
            }
        } catch (PatternSyntaxException e) {
            Log.add("Invalid regex pattern: " + pattern);
        }

        return groupList.toArray();
    }

    /** Extract named groups (if pattern uses named groups). */
    public Map<String, String> namedGroups(String text, String pattern) {
        Map<String, String> result = new HashMap<>();
        if (text == null || pattern == null) {
            return result;
        }

        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);

            if (m.find()) {
                // Try to extract named groups using reflection
                // Note: Java regex doesn't directly support named groups extraction
                // This is a simplified implementation
                int groupCount = m.groupCount();
                for (int i = 1; i <= groupCount; i++) {
                    result.put("group" + i, m.group(i));
                }
            }
        } catch (PatternSyntaxException e) {
            Log.add("Invalid regex pattern: " + pattern);
        }

        return result;
    }

    /** Check if pattern is valid. */
    public boolean isValid(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }
        try {
            Pattern.compile(pattern);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    /** Get match at specific index. */
    public String matchAt(String text, String pattern, int index) {
        if (text == null || pattern == null || index < 0) {
            return "";
        }

        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);

            int currentIndex = 0;
            while (m.find()) {
                if (currentIndex == index) {
                    return m.group();
                }
                currentIndex++;
            }
        } catch (PatternSyntaxException e) {
            Log.add("Invalid regex pattern: " + pattern);
        }

        return "";
    }

    /** Get all matches with their positions. */
    public Object[] matchesWithPositions(String text, String pattern) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (text == null || pattern == null) {
            return new Object[0];
        }

        try {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(text);

            while (m.find()) {
                Map<String, Object> match = new HashMap<>();
                match.put("text", m.group());
                match.put("start", m.start());
                match.put("end", m.end());
                results.add(match);
            }
        } catch (PatternSyntaxException e) {
            Log.add("Invalid regex pattern: " + pattern);
        }

        return results.toArray();
    }
}
