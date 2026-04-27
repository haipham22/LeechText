package dark.leech.text.plugin.js.api;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Rhino wrapper for JSoup Elements (collection). Simple Java class - public methods automatically
 * exposed to JavaScript. Supports array-like access via get(int) method.
 */
public class JSElements {
    private final Elements elements;

    /** Public length property for vBook compatibility - matches JSElements in vBooks Android */
    public int length;

    public JSElements(Elements elements) {
        this.elements = elements != null ? elements : new Elements();
        this.length = this.elements.size();
    }

    /**
     * Select from current elements using CSS selector. Returns new JSElements collection for
     * chaining.
     */
    public JSElements select(String selector) {
        try {
            dark.leech.text.action.Log.add(
                    "[JSElements.select()] Called with selector: " + selector);

            if (selector == null || selector.isEmpty()) {
                dark.leech.text.action.Log.add(
                        "[JSElements.select()] Selector is null/empty, returning empty JSElements");
                return new JSElements(new Elements());
            }

            Elements results = elements.select(selector);
            dark.leech.text.action.Log.add(
                    "[JSElements.select()] Found "
                            + results.size()
                            + " elements matching selector: "
                            + selector);

            return new JSElements(results);
        } catch (Exception e) {
            dark.leech.text.action.Log.add("[JSElements.select()] Exception: " + e.getMessage());
            return new JSElements(new Elements());
        }
    }

    /** Get concatenated text content of all elements. */
    public String text() {
        return elements.text();
    }

    /** Get concatenated HTML content of all elements. */
    public String html() {
        return elements.html();
    }

    /** Get concatenated outer HTML of all elements. */
    public String outerHtml() {
        return elements.outerHtml();
    }

    /** Get element at index - enables array-like access: links.get(0) */
    public JSElement get(int index) {
        if (index >= 0 && index < elements.size()) {
            return new JSElement(elements.get(index));
        }
        return null;
    }

    /** Get element at index (alias for get). */
    public JSElement eq(int index) {
        return get(index);
    }

    /** Get first element. */
    public JSElement first() {
        Element first = elements.first();
        return first != null ? new JSElement(first) : null;
    }

    /** Get last element. */
    public JSElement last() {
        Element last = elements.last();
        return last != null ? new JSElement(last) : null;
    }

    /** Get number of elements. */
    public int size() {
        dark.leech.text.action.Log.add("[JSElements.size()] Called, returning: " + elements.size());
        return elements.size();
    }

    /** Check if collection is empty. */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /** Get attribute of first element. */
    public String attr(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        Element first = elements.first();
        return first != null ? first.attr(key) : "";
    }

    /**
     * ForEach iteration - passes element and index to callback. Note: In Rhino, this receives a
     * Function object as the first argument.
     */
    public void forEach(Object callback) {
        if (callback instanceof org.mozilla.javascript.Function) {
            org.mozilla.javascript.Function func = (org.mozilla.javascript.Function) callback;
            org.mozilla.javascript.Context ctx = org.mozilla.javascript.Context.getCurrentContext();
            org.mozilla.javascript.Scriptable scope = func.getParentScope();

            for (int i = 0; i < elements.size(); i++) {
                JSElement element = new JSElement(elements.get(i));
                func.call(ctx, scope, scope, new Object[] {element, i});
            }
        }
    }

    /** Each iteration - jQuery style (index, element). */
    public void each(Object callback) {
        if (callback instanceof org.mozilla.javascript.Function) {
            org.mozilla.javascript.Function func = (org.mozilla.javascript.Function) callback;
            org.mozilla.javascript.Context ctx = org.mozilla.javascript.Context.getCurrentContext();
            org.mozilla.javascript.Scriptable scope = func.getParentScope();

            for (int i = 0; i < elements.size(); i++) {
                JSElement element = new JSElement(elements.get(i));
                func.call(ctx, scope, scope, new Object[] {i, element});
            }
        }
    }

    /**
     * Map - transform each element, returns JSList for vBook compatibility (matches vBook Android).
     */
    public JSList map(Object callback) {
        dark.leech.text.action.Log.add(
                "[JSElements.map()] Starting map operation, elements.size(): " + elements.size());

        JSList results = new JSList();
        if (callback instanceof org.mozilla.javascript.Function) {
            org.mozilla.javascript.Function func = (org.mozilla.javascript.Function) callback;
            org.mozilla.javascript.Context ctx = org.mozilla.javascript.Context.getCurrentContext();
            org.mozilla.javascript.Scriptable scope = func.getParentScope();

            for (int i = 0; i < elements.size(); i++) {
                JSElement element = new JSElement(elements.get(i));
                Object result = func.call(ctx, scope, scope, new Object[] {element, i});
                results.add(result);

                dark.leech.text.action.Log.add(
                        "[JSElements.map()] Processed element "
                                + i
                                + ", result: "
                                + (result != null ? result.getClass().getName() : "null"));
            }

            dark.leech.text.action.Log.add(
                    "[JSElements.map()] Completed map operation, JSList.size(): " + results.size());
        } else {
            dark.leech.text.action.Log.add(
                    "[JSElements.map()] Callback is not a Function: "
                            + (callback != null ? callback.getClass().getName() : "null"));
        }

        return results;
    }

    /** Convert to JavaScript array. */
    public Object[] toArray() {
        List<Object> array = new ArrayList<>();
        for (Element el : elements) {
            array.add(new JSElement(el));
        }
        return array.toArray();
    }

    /** Filter elements by predicate. */
    public JSElements filter(Object predicate) {
        if (predicate instanceof org.mozilla.javascript.Function) {
            org.mozilla.javascript.Function func = (org.mozilla.javascript.Function) predicate;
            org.mozilla.javascript.Context ctx = org.mozilla.javascript.Context.getCurrentContext();
            org.mozilla.javascript.Scriptable scope = func.getParentScope();

            Elements filtered = new Elements();
            for (int i = 0; i < elements.size(); i++) {
                JSElement element = new JSElement(elements.get(i));
                Object result = func.call(ctx, scope, scope, new Object[] {element, i});
                if (result instanceof Boolean && (Boolean) result) {
                    filtered.add(elements.get(i));
                }
            }
            return new JSElements(filtered);
        }
        return new JSElements(new Elements());
    }

    /** Find matching descendants. */
    public JSElements find(String selector) {
        return select(selector);
    }

    /** Get children of each element. */
    public JSElements children() {
        Elements allChildren = new Elements();
        for (Element el : elements) {
            allChildren.addAll(el.children());
        }
        return new JSElements(allChildren);
    }

    /** Get parents of each element. */
    public JSElements parents() {
        Elements allParents = new Elements();
        for (Element el : elements) {
            Element parent = el.parent();
            if (parent != null && !allParents.contains(parent)) {
                allParents.add(parent);
            }
        }
        return new JSElements(allParents);
    }

    /** Get next siblings. */
    public JSElements next() {
        Elements allNext = new Elements();
        for (Element el : elements) {
            Element next = el.nextElementSibling();
            if (next != null) {
                allNext.add(next);
            }
        }
        return new JSElements(allNext);
    }

    /** Get previous siblings. */
    public JSElements prev() {
        Elements allPrev = new Elements();
        for (Element el : elements) {
            Element prev = el.previousElementSibling();
            if (prev != null) {
                allPrev.add(prev);
            }
        }
        return new JSElements(allPrev);
    }

    /** Add class to all elements. */
    public JSElements addClass(String className) {
        for (Element el : elements) {
            el.addClass(className);
        }
        return this;
    }

    /** Remove class from all elements. */
    public JSElements removeClass(String className) {
        for (Element el : elements) {
            el.removeClass(className);
        }
        return this;
    }

    /** Remove elements. */
    public JSElements remove() {
        for (Element el : elements) {
            el.remove();
        }
        return this;
    }

    /** Remove matching elements. */
    public JSElements remove(String selector) {
        if (selector != null && !selector.isEmpty()) {
            elements.select(selector).remove();
        }
        return this;
    }
}
