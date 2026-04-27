package dark.leech.text.plugin.js.loader;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import dark.leech.text.enities.PluginEntity;

/**
 * Unit and integration tests for PageLoader. Tests the page discovery pattern for vBook plugins.
 */
public class PageLoaderTest {

    @Test
    public void testBasicPageDiscovery() {
        // Test basic page discovery returning array of URLs
        PluginEntity plugin =
                PluginEntity.builder()
                        .source("https://truyenfull.vision")
                        .pageGetter(
                                "function execute(url) {  return Response.success([   "
                                    + " 'https://truyenfull.vision/ajax.php?type=list_chapter&page=1',"
                                    + "    'https://truyenfull.vision/ajax.php?type=list_chapter&page=2',"
                                    + "    'https://truyenfull.vision/ajax.php?type=list_chapter&page=3'"
                                    + "  ]);}")
                        .build();

        PageLoader loader = PageLoader.with(plugin);
        List<String> urls = loader.load("https://truyenfull.vision/test-novel");

        assertNotNull("URL list should not be null", urls);
        assertEquals("Should discover 3 URLs", 3, urls.size());
        assertEquals(
                "First URL should match",
                "https://truyenfull.vision/ajax.php?type=list_chapter&page=1",
                urls.get(0));
    }

    @Test
    public void testEmptyResult() {
        // Test when script returns empty array
        PluginEntity plugin =
                PluginEntity.builder()
                        .source("https://test.com")
                        .pageGetter(
                                "function execute(url) {" + "  return Response.success([]);" + "}")
                        .build();

        PageLoader loader = PageLoader.with(plugin);
        List<String> urls = loader.load("https://test.com/test");

        assertNotNull("URL list should not be null", urls);
        assertTrue("URL list should be empty", urls.isEmpty());
    }

    @Test
    public void testNullResult() {
        // Test when script returns null
        PluginEntity plugin =
                PluginEntity.builder()
                        .source("https://test.com")
                        .pageGetter(
                                "function execute(url) {"
                                        + "  return Response.success(null);"
                                        + "}")
                        .build();

        PageLoader loader = PageLoader.with(plugin);
        List<String> urls = loader.load("https://test.com/test");

        assertNotNull("URL list should not be null", urls);
        assertTrue("URL list should be empty for null result", urls.isEmpty());
    }

    @Test
    public void testSingleUrlResult() {
        // Test when script returns single URL
        PluginEntity plugin =
                PluginEntity.builder()
                        .source("https://test.com")
                        .pageGetter(
                                "function execute(url) {"
                                        + "  return Response.success('https://test.com/page-1');"
                                        + "}")
                        .build();

        PageLoader loader = PageLoader.with(plugin);
        List<String> urls = loader.load("https://test.com/test");

        assertNotNull("URL list should not be null", urls);
        assertEquals("Should have 1 URL", 1, urls.size());
        assertEquals("URL should match", "https://test.com/page-1", urls.get(0));
    }

    @Test
    public void testTruyenFullPagePattern() {
        // Test real-world TruyenFull page.js pattern
        PluginEntity plugin =
                PluginEntity.builder()
                        .source("https://truyenfull.vision")
                        .pageGetter(
                                "function execute(url) {  var totalPages = 3;  var truyenId ="
                                    + " 'test-novel';  var list = [];  for (var i = 1; i <="
                                    + " totalPages; i++) {   "
                                    + " list.push('https://truyenfull.vision/ajax.php?type=list_chapter&tid='"
                                    + " + truyenId + '&page=' + i);  }  return"
                                    + " Response.success(list);}")
                        .build();

        PageLoader loader = PageLoader.with(plugin);
        List<String> urls = loader.load("https://truyenfull.vision/test-novel");

        assertNotNull("URL list should not be null", urls);
        assertEquals("Should discover 3 page URLs", 3, urls.size());
        assertTrue("First URL should contain page=1", urls.get(0).contains("page=1"));
        assertTrue("Last URL should contain page=3", urls.get(2).contains("page=3"));
    }

    @Test
    public void testNoPageGetterScript() {
        // Test when plugin has no pageGetter script
        PluginEntity plugin = PluginEntity.builder().source("https://test.com").build();

        PageLoader loader = PageLoader.with(plugin);
        List<String> urls = loader.load("https://test.com/test");

        assertNotNull("URL list should not be null", urls);
        assertTrue("URL list should be empty when no script", urls.isEmpty());
    }

    @Test
    public void testWithMixedUrlTypes() {
        // Test with various URL formats
        PluginEntity plugin =
                PluginEntity.builder()
                        .source("https://test.com")
                        .pageGetter(
                                "function execute(url) {"
                                        + "  return Response.success(["
                                        + "    'https://test.com/page-1',"
                                        + "    '/relative-url',"
                                        + "    'http://old-domain.com/page',"
                                        + "    'test.com/path'"
                                        + "  ]);"
                                        + "}")
                        .build();

        PageLoader loader = PageLoader.with(plugin);
        List<String> urls = loader.load("https://test.com/test");

        assertNotNull("URL list should not be null", urls);
        assertEquals("Should handle all URL types", 4, urls.size());
    }

    @Test
    public void testLargePageDiscovery() {
        // Test with large number of pages (performance test)
        PluginEntity plugin =
                PluginEntity.builder()
                        .source("https://test.com")
                        .pageGetter(
                                "function execute(url) {"
                                        + "  var list = [];"
                                        + "  for (var i = 1; i <= 100; i++) {"
                                        + "    list.push('https://test.com/page-' + i);"
                                        + "  }"
                                        + "  return Response.success(list);"
                                        + "}")
                        .build();

        PageLoader loader = PageLoader.with(plugin);
        List<String> urls = loader.load("https://test.com/test");

        assertNotNull("URL list should not be null", urls);
        assertEquals("Should discover 100 URLs", 100, urls.size());
        assertEquals("First URL should be page-1", "https://test.com/page-1", urls.get(0));
        assertEquals("Last URL should be page-100", "https://test.com/page-100", urls.get(99));
    }

    @Test
    public void testScriptErrorHandling() {
        // Test when script has syntax error
        PluginEntity plugin =
                PluginEntity.builder()
                        .source("https://test.com")
                        .pageGetter("function execute(url) {" + "  invalid javascript here" + "}")
                        .build();

        PageLoader loader = PageLoader.with(plugin);
        List<String> urls = loader.load("https://test.com/test");

        // Should handle error gracefully and return empty list
        assertNotNull("URL list should not be null on error", urls);
        assertTrue("URL list should be empty on error", urls.isEmpty());
    }

    @Test
    public void testObjectWithUrlProperty() {
        // Test when script returns object with url property
        PluginEntity plugin =
                PluginEntity.builder()
                        .source("https://test.com")
                        .pageGetter(
                                "function execute(url) {  return Response.success({url:"
                                        + " 'https://test.com/page-1'});}")
                        .build();

        PageLoader loader = PageLoader.with(plugin);
        List<String> urls = loader.load("https://test.com/test");

        assertNotNull("URL list should not be null", urls);
        assertEquals("Should extract URL from object", 1, urls.size());
        assertEquals("URL should match", "https://test.com/page-1", urls.get(0));
    }

    @Test
    public void testObjectWithLinkProperty() {
        // Test when script returns object with link property (fallback)
        PluginEntity plugin =
                PluginEntity.builder()
                        .source("https://test.com")
                        .pageGetter(
                                "function execute(url) {  return Response.success({link:"
                                        + " 'https://test.com/page-1'});}")
                        .build();

        PageLoader loader = PageLoader.with(plugin);
        List<String> urls = loader.load("https://test.com/test");

        assertNotNull("URL list should not be null", urls);
        assertEquals("Should extract link from object", 1, urls.size());
        assertEquals("Link should match", "https://test.com/page-1", urls.get(0));
    }
}
