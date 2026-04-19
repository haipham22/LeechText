package dark.leech.text.plugin.js.api;

import java.util.ArrayList;

/**
 * Special list type for vBook plugin compatibility using Rhino. Returned by Html.map() and supports
 * JavaScript array-like access. Simple Java class - public methods automatically exposed to
 * JavaScript.
 */
public class JSList extends ArrayList<Object> {

    public JSList() {
        super();
    }

    public JSList(int initialCapacity) {
        super(initialCapacity);
    }

    /** Add item to list. */
    @Override
    public boolean add(Object item) {
        return super.add(item);
    }

    /** Get item at index. */
    public Object get(int index) {
        if (index >= 0 && index < size()) {
            return super.get(index);
        }
        return null;
    }

    /** Convert to JavaScript array. */
    public Object[] toArray() {
        return super.toArray();
    }

    /** ForEach iteration - passes element and index to callback. */
    public void forEach(Object callback) {
        if (callback instanceof org.mozilla.javascript.Function) {
            org.mozilla.javascript.Function func = (org.mozilla.javascript.Function) callback;
            org.mozilla.javascript.Context ctx = org.mozilla.javascript.Context.getCurrentContext();
            org.mozilla.javascript.Scriptable scope = func.getParentScope();

            for (int i = 0; i < size(); i++) {
                Object item = get(i);
                func.call(ctx, scope, scope, new Object[] {item, i});
            }
        }
    }

    /** Map - transform each element, returns new JSList. */
    public JSList map(Object callback) {
        JSList result = new JSList(size());
        if (callback instanceof org.mozilla.javascript.Function) {
            org.mozilla.javascript.Function func = (org.mozilla.javascript.Function) callback;
            org.mozilla.javascript.Context ctx = org.mozilla.javascript.Context.getCurrentContext();
            org.mozilla.javascript.Scriptable scope = func.getParentScope();

            for (int i = 0; i < size(); i++) {
                Object item = get(i);
                Object mapped = func.call(ctx, scope, scope, new Object[] {item, i});
                result.add(mapped);
            }
        }
        return result;
    }

    /** Filter elements by predicate. */
    public JSList filter(Object predicate) {
        JSList result = new JSList();
        if (predicate instanceof org.mozilla.javascript.Function) {
            org.mozilla.javascript.Function func = (org.mozilla.javascript.Function) predicate;
            org.mozilla.javascript.Context ctx = org.mozilla.javascript.Context.getCurrentContext();
            org.mozilla.javascript.Scriptable scope = func.getParentScope();

            for (int i = 0; i < size(); i++) {
                Object item = get(i);
                Object testResult = func.call(ctx, scope, scope, new Object[] {item, i});
                if (testResult instanceof Boolean && (Boolean) testResult) {
                    result.add(item);
                }
            }
        }
        return result;
    }

    /** Find first element matching predicate. */
    public Object find(Object predicate) {
        if (predicate instanceof org.mozilla.javascript.Function) {
            org.mozilla.javascript.Function func = (org.mozilla.javascript.Function) predicate;
            org.mozilla.javascript.Context ctx = org.mozilla.javascript.Context.getCurrentContext();
            org.mozilla.javascript.Scriptable scope = func.getParentScope();

            for (int i = 0; i < size(); i++) {
                Object item = get(i);
                Object testResult = func.call(ctx, scope, scope, new Object[] {item, i});
                if (testResult instanceof Boolean && (Boolean) testResult) {
                    return item;
                }
            }
        }
        return null;
    }

    /** Join elements with separator. */
    public String join(String separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size(); i++) {
            if (i > 0) {
                sb.append(separator);
            }
            Object item = get(i);
            if (item != null) {
                sb.append(org.mozilla.javascript.Context.toString(item));
            }
        }
        return sb.toString();
    }

    /** Check if list contains element. */
    public boolean contains(Object element) {
        return super.contains(element);
    }

    /** Get first element. */
    public Object first() {
        return isEmpty() ? null : get(0);
    }

    /** Get last element. */
    public Object last() {
        return isEmpty() ? null : get(size() - 1);
    }

    /** Check if list is empty. */
    public boolean isEmpty() {
        return super.isEmpty();
    }
}
