package dark.leech.text.plugin.js.api;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import dark.leech.text.action.Log;

/**
 * Rhino wrapper for JSoup Element. Simple Java class - public methods automatically exposed to
 * JavaScript.
 */
public class JSElement {
    private final Element element;

    public JSElement(Element element) {
        this.element = element;
    }

    /** Select child elements using CSS selector. Returns JSElements collection for chaining. */
    public JSElements select(String selector) {
        try {
            if (element == null) {
                Log.add(
                        "WARNING: JSElement.select() called on null element with selector: "
                                + selector);
                return new JSElements(new Elements());
            }
            if (selector == null || selector.isEmpty()) {
                return new JSElements(new Elements());
            }
            Elements results = element.select(selector);
            return new JSElements(results);
        } catch (Exception e) {
            Log.add(
                    "ERROR: JSElement.select() failed for selector '"
                            + selector
                            + "': "
                            + e.getMessage());
            return new JSElements(new Elements());
        }
    }

    /** Get element text content. */
    public String text() {
        return element != null ? element.text() : "";
    }

    /** Get element own text (excluding children). */
    public String ownText() {
        return element != null ? element.ownText() : "";
    }

    /** Get element HTML content. */
    public String html() {
        return element != null ? element.html() : "";
    }

    /** Get element outer HTML. */
    public String outerHtml() {
        return element != null ? element.outerHtml() : "";
    }

    /** Get attribute value. */
    public String attr(String key) {
        if (key == null || key.isEmpty()) {
            return "";
        }
        return element != null ? element.attr(key) : "";
    }

    /** Set attribute value. */
    public JSElement attr(String key, String value) {
        if (element != null && key != null && !key.isEmpty()) {
            element.attr(key, value);
        }
        return this;
    }

    /** Check if element has class. */
    public boolean hasClass(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }
        return element != null && element.hasClass(className);
    }

    /** Get element tag name. */
    public String tagName() {
        return element != null ? element.tagName() : "";
    }

    /** Get element id. */
    public String id() {
        return element != null ? element.id() : "";
    }

    /** Get element class name. */
    public String className() {
        return element != null ? element.className() : "";
    }

    /** Get first child element. */
    public JSElement first() {
        if (element == null) {
            return null;
        }
        Element first = element.firstElementChild();
        return first != null ? new JSElement(first) : null;
    }

    /** Get last child element. */
    public JSElement last() {
        if (element == null) {
            return null;
        }
        Element last = element.lastElementChild();
        return last != null ? new JSElement(last) : null;
    }

    /** Get child elements. */
    public JSElements children() {
        if (element == null) {
            return new JSElements(new Elements());
        }
        return new JSElements(element.children());
    }

    /** Get parent element. */
    public JSElement parent() {
        if (element == null) {
            return null;
        }
        Element parent = element.parent();
        return parent != null ? new JSElement(parent) : null;
    }

    /** Get next sibling element. */
    public JSElement next() {
        if (element == null) {
            return null;
        }
        Element next = element.nextElementSibling();
        return next != null ? new JSElement(next) : null;
    }

    /** Get previous sibling element. */
    public JSElement prev() {
        if (element == null) {
            return null;
        }
        Element prev = element.previousElementSibling();
        return prev != null ? new JSElement(prev) : null;
    }

    /** Check if element is empty (no children). */
    public boolean isEmpty() {
        return element == null || element.children().isEmpty();
    }

    /** Get number of child elements. */
    public int childrenSize() {
        return element != null ? element.children().size() : 0;
    }

    /** Remove element. */
    public void remove() {
        if (element != null) {
            element.remove();
        }
    }

    /** Remove elements matching selector. */
    public JSElement remove(String selector) {
        if (element != null && selector != null && !selector.isEmpty()) {
            element.select(selector).remove();
        }
        return this;
    }

    /** Add class. */
    public JSElement addClass(String className) {
        if (element != null && className != null && !className.isEmpty()) {
            element.addClass(className);
        }
        return this;
    }

    /** Remove class. */
    public JSElement removeClass(String className) {
        if (element != null && className != null && !className.isEmpty()) {
            element.removeClass(className);
        }
        return this;
    }

    /** Toggle class. */
    public void toggleClass(String className) {
        if (element != null && className != null && !className.isEmpty()) {
            element.toggleClass(className);
        }
    }
}
