package dark.leech.text.get;

import java.util.ArrayList;

import dark.leech.text.models.Post;

/**
 * Legacy interface for Java-based page pagination.
 *
 * @deprecated Use {@link dark.leech.text.plugin.js.loader.PageLoader} with JavaScript page.js
 *     scripts instead. This interface uses reflection and is no longer recommended.
 * @since 1.0
 * @see PageLoader
 */
@Deprecated
public interface PageGetter {
    /**
     * Get posts from the given URL.
     *
     * @param url Target URL
     * @return List of posts
     * @deprecated Use PageLoader with page.js scripts instead
     */
    @Deprecated
    ArrayList<Post> getter(String url);
}
