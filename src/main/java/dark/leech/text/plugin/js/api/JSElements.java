package dark.leech.text.plugin.js.api;

import java.util.ArrayList;
import java.util.List;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Rhino wrapper for JSoup Elements (collection). Simple Java class - public methods automatically
 * exposed to JavaScript.
 */
public class JSElements {
    private final Elements elements;

    public JSElements(Elements elements) {
        this.elements = elements != null ? elements : new Elements();
    }

    /**
     * Select from current elements using CSS selector. Returns new JSElements collection for
     * chaining.
     */
    public JSElements select(String selector) {
        try {
            if (selector == null || selector.isEmpty()) {
                return new JSElements(new Elements());
            }
            Elements results = elements.select(selector);
            return new JSElements(results);
        } catch (Exception e) {
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

    /** Get element at index. */
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

    /** Map - transform each element, returns array. */
    public Object[] map(Object callback) {
        if (callback instanceof org.mozilla.javascript.Function) {
            org.mozilla.javascript.Function func = (org.mozilla.javascript.Function) callback;
            org.mozilla.javascript.Context ctx = org.mozilla.javascript.Context.getCurrentContext();
            org.mozilla.javascript.Scriptable scope = func.getParentScope();

            List<Object> results = new ArrayList<>();
            for (int i = 0; i < elements.size(); i++) {
                JSElement element = new JSElement(elements.get(i));
                Object result = func.call(ctx, scope, scope, new Object[] {element, i});
                results.add(result);
            }
            return results.toArray();
        }
        return new Object[0];
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
