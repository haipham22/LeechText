package dark.leech.text.plugin.js.api;

/**
 * Functional interface for consuming values in forEach/map operations. Compatible with vBook plugin
 * callback pattern using Rhino.
 */
@FunctionalInterface
public interface Consumer {
    /**
     * Accept a value and optionally transform it.
     *
     * @param value The input value (automatically converted by Rhino)
     * @return The transformed value (or null for void operations)
     */
    Object accept(Object value);
}
