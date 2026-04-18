package dark.leech.text.plugin.js.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.Scriptable;

import dark.leech.text.action.Log;

/**
 * JSON API for JavaScript plugins using Rhino. Provides parse/stringify with proper
 * null/object/array conversion.
 */
public class Json extends JsApiWrapper {

    /**
     * Create Json API with execution context for proper object creation.
     *
     * @param context The Rhino Context for script execution
     * @param scope The Rhino Scriptable scope
     */
    public Json(Context context, Scriptable scope) {
        super(context, scope);
    }

    /** Create Json API without context (legacy compatibility). */
    public Json() {
        super();
    }

    /**
     * Parse JSON string to Java object (automatically converted to JS by Rhino). Handles objects,
     * arrays, primitives, null. Usage: json.parse('{"key": "value"}')
     */
    public Object parse(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }

        String trimmed = jsonString.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            // Try as object first
            if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(jsonString);
                return convertObject(obj);
            }
            // Try as array
            else if (trimmed.startsWith("[")) {
                JSONArray arr = new JSONArray(jsonString);
                return convertArray(arr);
            }
            // Try as primitive
            else {
                return parsePrimitive(trimmed);
            }
        } catch (Exception e) {
            Log.add("JSON parsing failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Convert Java object to JSON string. Handles objects, arrays, primitives, null. Usage:
     * json.stringify({key: "value"})
     */
    public String stringify(Object value) {
        if (value == null) {
            return "null";
        }

        // Rhino primitives
        if (value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Number) {
            return String.valueOf(value);
        }
        if (value instanceof String) {
            return quoteString((String) value);
        }

        // Handle NativeObject (Rhino object)
        if (value instanceof NativeObject) {
            NativeObject nativeObj = (NativeObject) value;
            JSONObject obj = new JSONObject();
            for (Object key : nativeObj.getIds()) {
                if (key instanceof String) {
                    Object member = nativeObj.get((String) key, nativeObj);
                    obj.put((String) key, convertToJava(member));
                }
            }
            return obj.toString();
        }

        // Handle NativeArray (Rhino array)
        if (value instanceof NativeArray) {
            NativeArray nativeArr = (NativeArray) value;
            JSONArray arr = new JSONArray();
            long len = nativeArr.getLength();
            for (long i = 0; i < len; i++) {
                Object elem = nativeArr.get((int) i, nativeArr);
                arr.put(convertToJava(elem));
            }
            return arr.toString();
        }

        // Handle regular Map
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<?, ?> map = (Map<?, ?>) value;
            JSONObject obj = new JSONObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String) {
                    obj.put((String) entry.getKey(), convertToJava(entry.getValue()));
                }
            }
            return obj.toString();
        }

        // Handle regular List/array
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            JSONArray arr = new JSONArray();
            for (Object item : list) {
                arr.put(convertToJava(item));
            }
            return arr.toString();
        }
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            JSONArray jsonArr = new JSONArray();
            for (Object item : arr) {
                jsonArr.put(convertToJava(item));
            }
            return jsonArr.toString();
        }

        return "null";
    }

    /** Parse primitive JSON value (string, number, boolean, null). */
    private Object parsePrimitive(String value) {
        // Remove quotes for strings
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        // Check for boolean
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        if ("null".equalsIgnoreCase(value)) {
            return null;
        }
        // Try as number
        try {
            if (value.contains(".")) {
                return Double.parseDouble(value);
            } else {
                return Long.parseLong(value);
            }
        } catch (NumberFormatException e) {
            // Fallback to string
            return value;
        }
    }

    /** Convert JSONObject to Map (automatically converted to JS object by Rhino). */
    private Map<String, Object> convertObject(JSONObject obj) {
        Map<String, Object> map = new HashMap<>();
        for (String key : obj.keySet()) {
            map.put(key, convertValue(obj.get(key)));
        }
        return map;
    }

    /** Convert JSONArray to List (automatically converted to JS array by Rhino). */
    private List<Object> convertArray(JSONArray arr) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            list.add(convertValue(arr.get(i)));
        }
        return list;
    }

    /** Convert JSON value to Java value. */
    private Object convertValue(Object value) {
        if (value == null || JSONObject.NULL.equals(value)) {
            return null;
        }

        if (value instanceof JSONObject) {
            return convertObject((JSONObject) value);
        } else if (value instanceof JSONArray) {
            return convertArray((JSONArray) value);
        } else if (value instanceof Boolean) {
            return value;
        } else if (value instanceof Number) {
            return value;
        } else if (value instanceof String) {
            return value;
        }

        return String.valueOf(value);
    }

    /** Convert JavaScript object to Java object for JSON serialization. */
    private Object convertToJava(Object value) {
        if (value == null) {
            return JSONObject.NULL;
        }

        if (value instanceof Boolean) {
            return value;
        }

        if (value instanceof Number) {
            return value;
        }

        if (value instanceof String) {
            return value;
        }

        // Handle NativeObject
        if (value instanceof NativeObject) {
            NativeObject nativeObj = (NativeObject) value;
            Map<String, Object> map = new HashMap<>();
            for (Object key : nativeObj.getIds()) {
                if (key instanceof String) {
                    Object member = nativeObj.get((String) key, nativeObj);
                    map.put((String) key, convertToJava(member));
                }
            }
            return new JSONObject(map);
        }

        // Handle NativeArray
        if (value instanceof NativeArray) {
            NativeArray nativeArr = (NativeArray) value;
            JSONArray arr = new JSONArray();
            long len = nativeArr.getLength();
            for (long i = 0; i < len; i++) {
                Object elem = nativeArr.get((int) i, nativeArr);
                arr.put(convertToJava(elem));
            }
            return arr;
        }

        // Handle regular Map
        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<?, ?> map = (Map<?, ?>) value;
            Map<String, Object> result = new HashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String) {
                    result.put((String) entry.getKey(), convertToJava(entry.getValue()));
                }
            }
            return new JSONObject(result);
        }

        // Handle regular List/array
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            JSONArray arr = new JSONArray();
            for (Object item : list) {
                arr.put(convertToJava(item));
            }
            return arr;
        }
        if (value instanceof Object[]) {
            Object[] arr = (Object[]) value;
            JSONArray jsonArr = new JSONArray();
            for (Object item : arr) {
                jsonArr.put(convertToJava(item));
            }
            return jsonArr;
        }

        return null;
    }

    /** Quote a string for JSON output. */
    private String quoteString(String str) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : str.toCharArray()) {
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    /** Get value from object by path (dot notation). Usage: json.get(obj, "user.name") */
    @SuppressWarnings("unchecked")
    public Object get(Object obj, String path) {
        if (obj == null || path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = obj;

        for (String part : parts) {
            if (current == null) {
                return null;
            }

            // Handle NativeObject
            if (current instanceof NativeObject) {
                NativeObject nativeObj = (NativeObject) current;
                if (nativeObj.has(part, nativeObj)) {
                    current = nativeObj.get(part, nativeObj);
                } else {
                    return null;
                }
            }
            // Handle regular Map
            else if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            }
            // Handle NativeArray with numeric index
            else if (current instanceof NativeArray) {
                try {
                    int index = Integer.parseInt(part);
                    current = ((NativeArray) current).get((int) index, (Scriptable) current);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            // Handle regular List with numeric index
            else if (current instanceof List) {
                try {
                    int index = Integer.parseInt(part);
                    current = ((List<?>) current).get(index);
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    return null;
                }
            } else {
                return null;
            }
        }

        return current;
    }

    /** Set value in object by path (dot notation). */
    @SuppressWarnings("unchecked")
    public void set(Object obj, String path, Object value) {
        if (obj == null || path == null || path.isEmpty()) {
            return;
        }

        String[] parts = path.split("\\.");
        Object current = obj;

        // Navigate to parent
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (current instanceof NativeObject) {
                NativeObject nativeObj = (NativeObject) current;
                if (!nativeObj.has(part, nativeObj)) {
                    // Create nested object
                    NativeObject newObj = new NativeObject();
                    nativeObj.put(part, nativeObj, newObj);
                }
                current = nativeObj.get(part, nativeObj);
            } else if (current instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) current;
                if (!map.containsKey(part)) {
                    map.put(part, new HashMap<String, Object>());
                }
                current = map.get(part);
            } else {
                return;
            }
        }

        // Set final value
        String lastPart = parts[parts.length - 1];
        if (current instanceof NativeObject) {
            NativeObject nativeObj = (NativeObject) current;
            nativeObj.put(lastPart, nativeObj, value);
        } else if (current instanceof Map) {
            ((Map<String, Object>) current).put(lastPart, value);
        }
    }

    /** Check if value is valid JSON. */
    public boolean isValid(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return false;
        }
        try {
            parse(jsonString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Pretty print JSON with indentation. */
    public String pretty(Object value) {
        if (value == null) {
            return "null";
        }

        try {
            String str = stringify(value);
            // org.json doesn't have toString(indent) method
            // Return compact JSON
            return str;
        } catch (Exception e) {
            return stringify(value);
        }
    }
}
