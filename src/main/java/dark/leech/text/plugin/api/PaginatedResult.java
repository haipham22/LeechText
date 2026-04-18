package dark.leech.text.plugin.api;

import java.util.List;

/**
 * Paginated result container for search and listing operations. Handles large result sets
 * efficiently.
 */
public class PaginatedResult<T> {
    private final List<T> items;
    private final int totalCount;
    private final int page;
    private final int pageSize;
    private final int totalPages;
    private final boolean hasMore;

    private PaginatedResult(Builder<T> builder) {
        this.items = builder.items;
        this.totalCount = builder.totalCount;
        this.page = builder.page;
        this.pageSize = builder.pageSize;
        this.totalPages = builder.totalPages;
        this.hasMore = builder.hasMore;
    }

    public List<T> getItems() {
        return items;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean hasMore() {
        return hasMore;
    }

    /** Create paginated result from items and total count. */
    public static <T> PaginatedResult<T> of(List<T> items, int totalCount, int page, int pageSize) {
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        boolean hasMore = page < totalPages - 1;

        return new Builder<T>()
                .items(items)
                .totalCount(totalCount)
                .page(page)
                .pageSize(pageSize)
                .totalPages(totalPages)
                .hasMore(hasMore)
                .build();
    }

    /** Create empty result. */
    public static <T> PaginatedResult<T> empty() {
        return new Builder<T>()
                .items(List.of())
                .totalCount(0)
                .page(0)
                .pageSize(0)
                .totalPages(0)
                .hasMore(false)
                .build();
    }

    /** Get next page number, or -1 if no more pages. */
    public int getNextPage() {
        return hasMore ? page + 1 : -1;
    }

    /** Get previous page number, or -1 if at first page. */
    public int getPreviousPage() {
        return page > 0 ? page - 1 : -1;
    }

    public static <T> Builder<T> builder() {
        return new Builder<T>();
    }

    public static class Builder<T> {
        private List<T> items = List.of();
        private int totalCount = 0;
        private int page = 0;
        private int pageSize = 20;
        private int totalPages = 0;
        private boolean hasMore = false;

        public Builder<T> items(List<T> items) {
            this.items = items;
            return this;
        }

        public Builder<T> totalCount(int totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public Builder<T> page(int page) {
            this.page = page;
            return this;
        }

        public Builder<T> pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder<T> totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public Builder<T> hasMore(boolean hasMore) {
            this.hasMore = hasMore;
            return this;
        }

        public PaginatedResult<T> build() {
            return new PaginatedResult<>(this);
        }
    }
}
