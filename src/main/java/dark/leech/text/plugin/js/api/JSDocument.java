package dark.leech.text.plugin.js.api;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import dark.leech.text.action.Log;

/**
 * Rhino wrapper for JSoup Document. Provides select() method returning JSElements. Simple Java
 * class - public methods automatically exposed to JavaScript.
 */
public class JSDocument {
    private final org.jsoup.nodes.Document document;

    public JSDocument(org.jsoup.nodes.Document document) {
        this.document = document;
    }

    /** Select elements using CSS selector. Returns JSElements collection for chaining. */
    public JSElements select(String selector) {
        try {
            Log.add(
                    "[JSDocument.select()] Called with selector: "
                            + selector
                            + ", document: "
                            + document);

            if (document == null) {
                Log.add("[JSDocument.select()] Document is null, returning empty JSElements");
                return new JSElements(new Elements());
            }
            if (selector == null || selector.isEmpty()) {
                Log.add("[JSDocument.select()] Selector is null/empty, returning empty JSElements");
                return new JSElements(new Elements());
            }

            Elements results = document.select(selector);
            Log.add(
                    "[JSDocument.select()] Found "
                            + results.size()
                            + " elements matching selector: "
                            + selector);

            return new JSElements(results);
        } catch (Exception e) {
            Log.add("[JSDocument.select()] Exception: " + e.getMessage());
            return new JSElements(new Elements());
        }
    }

    /** Get document text content. */
    public String text() {
        return document != null ? document.text() : "";
    }

    /** Get document HTML content. */
    public String html() {
        return document != null ? document.html() : "";
    }

    /** Get document title. */
    public String title() {
        return document != null ? document.title() : "";
    }

    /** Get body element. */
    public JSElement body() {
        Element body = document != null ? document.body() : null;
        return body != null ? new JSElement(body) : null;
    }

    /** Get head element. */
    public JSElement head() {
        Element head = document != null ? document.head() : null;
        return head != null ? new JSElement(head) : null;
    }

    /** Get element by ID. */
    public JSElement getElementById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        Element element = document.getElementById(id);
        return element != null ? new JSElement(element) : null;
    }
}
