package dark.leech.text.plugin.js.loader;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Undefined;

/**
 * JavaScript response converter for vBook plugins using Rhino. Converts Rhino
 * NativeObject/NativeArray to plain Java Map/List for entity extraction. Based on vBooks-decompiled
 * JSResponse pattern.
 */
public final class JSResponse {

    private JSResponse() {
        // Utility class - prevent instantiation
    }

    /**
     * Convert NativeObject to plain Java Map. Recursively handles nested NativeObject and
     * NativeArray.
     */
    public static Map<String, Object> convertObject(NativeObject nativeObject) {
        if (nativeObject == null) {
            return new HashMap<>();
        }

        Map<String, Object> map = new LinkedHashMap<>();
        for (Object key : nativeObject.keySet()) {
            Object value = nativeObject.get(key);
            String keyStr =
                    (key instanceof Number)
                            ? String.valueOf(((Number) key).intValue())
                            : String.valueOf(key);
            map.put(keyStr, convertValue(value));
        }
        return map;
    }

    /**
     * Convert NativeArray to plain Java Object[]. Recursively handles nested NativeObject and
     * NativeArray.
     */
    public static Object[] convertArray(NativeArray nativeArray) {
        if (nativeArray == null) {
            return new Object[0];
        }

        Object[] array = new Object[(int) nativeArray.getLength()];
        for (int i = 0; i < nativeArray.getLength(); i++) {
            Object value = nativeArray.get(i);
            array[i] = convertValue(value);
        }
        return array;
    }

    /**
     * Convert any Rhino JavaScript value to plain Java value. Handles: NativeObject, NativeArray,
     * Undefined, null, primitives.
     */
    public static Object convertValue(Object value) {
        if (value == null || value == Undefined.instance) {
            return null;
        }

        if (value instanceof NativeObject) {
            return convertObject((NativeObject) value);
        }

        if (value instanceof NativeArray) {
            return convertArray((NativeArray) value);
        }

        // Return primitives directly (String, Number, Boolean)
        return value;
    }

    /** Safely get string value from object. Returns null if object is null or Undefined. */
    public static String getString(Object obj) {
        if (obj == null || obj == Undefined.instance) {
            return null;
        }

        if (obj instanceof NativeObject) {
            // Don't auto-convert objects to strings
            return null;
        }

        if (obj instanceof NativeArray object) {
            // Don't auto-convert arrays to strings
            return object.toString();
        }

        if (obj instanceof Function) {
            // Functions can't be safely converted to strings
            return null;
        }

        if (obj instanceof NativeJavaObject object) {
            // Unwrap to get the actual Java object instead of "[object Object]"
            Object unwrapped = object.unwrap();
            return unwrapped != null ? unwrapped.toString() : null;
        }

        // UNIVERSAL PATTERN: Use Context.toString() for all Rhino object conversions
        // This is the SAFE way to convert ANY JavaScript object to string
        try {
            return org.mozilla.javascript.Context.toString(obj);
        } catch (Exception e) {
            // Final fallback - if Context.toString() fails, return null
            return null;
        }
    }

    /**
     * UNIVERSAL STRING CONVERSION UTILITY
     *
     * <p>Use this method for ALL JavaScript-to-Java string conversions. This is the meta-pattern
     * solution for toString() errors across all JavaScript APIs.
     *
     * <p>Meta-Pattern: JavaScript objects (especially Functions) cannot use Java's .toString()
     * Solution: Always use Context.toString() for Rhino object string conversion
     *
     * <p>Usage: Replace all obj.toString() calls with JSResponse.safeToString(obj)
     */
    public static String safeToString(Object obj) {
        return getString(obj);
    }

    /**
     * Get nested property value from NativeObject. Returns null if any path component is missing.
     */
    public static Object getProperty(NativeObject obj, String... path) {
        if (obj == null || path == null || path.length == 0) {
            return null;
        }

        Object current = obj;
        for (String key : path) {
            if (current instanceof NativeObject) {
                current = ((NativeObject) current).get(key);
            } else {
                return null;
            }

            if (current == null || current == Undefined.instance) {
                return null;
            }
        }

        return current;
    }
}
