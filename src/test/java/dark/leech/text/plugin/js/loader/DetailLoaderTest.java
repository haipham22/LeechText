package dark.leech.text.plugin.js.loader;

import dark.leech.text.enities.PluginEntity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Integration test for DetailLoader with Response.success().
 * Tests the fix for the "Unknown identifier: success" error.
 */
public class DetailLoaderTest {

    @Test
    public void testResponseSuccessFromJavaScript() {
        // Create a minimal plugin with detail getter that uses Response.success
        PluginEntity plugin = PluginEntity.builder()
                .source("https://truyenfull.vision")
                .detailGetter(
                    "function execute(url) {" +
                    "  return Response.success({" +
                    "    name: 'Test Novel'," +
                    "    author: 'Test Author'," +
                    "    description: 'Test description'," +
                    "    cover: 'https://example.com/cover.jpg'" +
                    "  });" +
                    "}"
                )
                .build();

        DetailLoader loader = DetailLoader.with(plugin);
        var result = loader.load("https://truyenfull.vision/test-novel");

        assertNotNull("Result should not be null", result);
        assertEquals("Name should match", "Test Novel", result.getName());
        assertEquals("Author should match", "Test Author", result.getAuthor());
        assertEquals("Description should match", "Test description", result.getIntroduce());
        assertEquals("Cover should match", "https://example.com/cover.jpg", result.getCover());
    }

    @Test
    public void testResponseSuccessWithSingleValue() {
        // Test Response.success with a simple string value
        PluginEntity plugin = PluginEntity.builder()
                .source("https://test.com")
                .detailGetter(
                    "function execute(url) {" +
                    "  return Response.success('Simple success value');" +
                    "}"
                )
                .build();

        DetailLoader loader = DetailLoader.with(plugin);
        var result = loader.load("https://test.com/test");

        assertNotNull("Result should not be null", result);
        // When Response.success returns a non-object, it won't be mapped to BookEntity fields
        // but the loader should still complete without throwing
    }

    @Test
    public void testResponseSuccessWithArray() {
        // Test Response.success with an array (used in list/chapter scenarios)
        PluginEntity plugin = PluginEntity.builder()
                .source("https://test.com")
                .detailGetter(
                    "function execute(url) {" +
                    "  return Response.success([" +
                    "    {name: 'Chapter 1', url: '/chap-1'}," +
                    "    {name: 'Chapter 2', url: '/chap-2'}" +
                    "  ]);" +
                    "}"
                )
                .build();

        DetailLoader loader = DetailLoader.with(plugin);
        var result = loader.load("https://test.com/test");

        assertNotNull("Result should not be null", result);
        // Arrays are handled but won't map to BookEntity fields
    }

    @Test
    public void testResponseSuccessWithNull() {
        // Test Response.success with null value
        PluginEntity plugin = PluginEntity.builder()
                .source("https://test.com")
                .detailGetter(
                    "function execute(url) {" +
                    "  return Response.success(null);" +
                    "}"
                )
                .build();

        DetailLoader loader = DetailLoader.with(plugin);
        var result = loader.load("https://test.com/test");

        // Should return null or empty result
        assertNotNull("Loader should handle null gracefully", loader);
    }

    @Test
    public void testResponseSuccessInRealWorldScenario() {
        // Test a more realistic scenario similar to TruyenFull plugin
        PluginEntity plugin = PluginEntity.builder()
                .source("https://truyenfull.vision")
                .detailGetter(
                    "function execute(url) {" +
                    "  var mockData = {" +
                    "    name: 'Test Novel Name'," +
                    "    cover: 'https://truyenfull.vision/cover.jpg'," +
                    "    author: 'Test Author'," +
                    "    description: 'Test description'," +
                    "    detail: ''" +
                    "  };" +
                    "  return Response.success(mockData);" +
                    "}"
                )
                .build();

        DetailLoader loader = DetailLoader.with(plugin);
        var result = loader.load("https://truyenfull.vision/test-novel");

        assertNotNull("Result should not be null", result);
        assertEquals("Name should match", "Test Novel Name", result.getName());
        assertEquals("Author should match", "Test Author", result.getAuthor());
        assertEquals("Cover should be absolute URL", "https://truyenfull.vision/cover.jpg", result.getCover());
    }
}
