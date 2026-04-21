package dark.leech.text.plugin.js.loader;

import java.util.Collections;
import java.util.List;

/**
 * Result model for paginated plugin responses.
 * Holds items list plus pagination metadata (next page identifier).
 * Immutable - use Builder for construction.
 *
 * @param <T> Type of items in the result
 */
public final class PaginationResult<T> {

    private final List<T> items;
    private final String nextPage;
    private final boolean hasNext;

    public List<T> getItems() {
        return items;
    }

    public String getNextPage() {
        return nextPage;
    }

    public boolean hasNext() {
        return hasNext;
    }

    private PaginationResult(Builder<T> builder) {
        this.items = builder.items == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(builder.items);
        this.nextPage = builder.nextPage;
        this.hasNext = builder.nextPage != null && !builder.nextPage.isEmpty();
    }

    /**
     * Create empty result with no items and no next page.
     */
    public static <T> PaginationResult<T> empty() {
        return new Builder<T>().build();
    }

    /**
     * Create result with items only (no pagination).
     */
    public static <T> PaginationResult<T> of(List<T> items) {
        return new Builder<T>().items(items).build();
    }

    /**
     * Create result with items and next page.
     */
    public static <T> PaginationResult<T> of(List<T> items, String nextPage) {
        return new Builder<T>().items(items).nextPage(nextPage).build();
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private List<T> items;
        private String nextPage;

        public Builder<T> items(List<T> items) {
            this.items = items;
            return this;
        }

        public Builder<T> nextPage(String nextPage) {
            this.nextPage = nextPage;
            return this;
        }

        public PaginationResult<T> build() {
            return new PaginationResult<>(this);
        }
    }
}
