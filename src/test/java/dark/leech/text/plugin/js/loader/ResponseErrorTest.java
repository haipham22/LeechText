package dark.leech.text.plugin.js.loader;

import org.junit.Test;
import dark.leech.text.enities.PluginEntity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for Response.error() methods. Validates error handling in vBooks compatibility
 * mode.
 */
public class ResponseErrorTest {

    @Test
    public void testResponseErrorWithData() {
        // Create plugin with error response
        PluginEntity plugin = PluginEntity.builder()
                .source("https://test.com")
                .detailGetter(
                    "function execute(url) {" +
                    "  return Response.error('Book not found');" +
                    "}"
                )
                .build();

        DetailLoader loader = DetailLoader.with(plugin);
        var result = loader.load("https://test.com/test-book");

        // Should return empty BookEntity on error
        assertNotNull("Result should not be null", result);
        // getName() may return null or empty string on error
        String name = result.getName();
        assertTrue("Name should be null or empty on error", name == null || name.isEmpty());
    }

    @Test
    public void testResponseErrorWithCode() {
        PluginEntity plugin = PluginEntity.builder()
                .source("https://test.com")
                .detailGetter(
                    "function execute(url) {" +
                    "  return Response.error(404, 'Not found');" +
                    "}"
                )
                .build();

        DetailLoader loader = DetailLoader.with(plugin);
        var result = loader.load("https://test.com/test-book");

        // Should return empty BookEntity on error
        assertNotNull("Result should not be null", result);
        String name = result.getName();
        assertTrue("Name should be null or empty on error", name == null || name.isEmpty());
    }

    @Test
    public void testResponseErrorWithCodeAndMessage() {
        PluginEntity plugin = PluginEntity.builder()
                .source("https://test.com")
                .detailGetter(
                    "function execute(url) {" +
                    "  return Response.error(500, 'Server error', 'Internal error');" +
                    "}"
                )
                .build();

        DetailLoader loader = DetailLoader.with(plugin);
        var result = loader.load("https://test.com/test-book");

        // Should return empty BookEntity on error
        assertNotNull("Result should not be null", result);
        String name = result.getName();
        assertTrue("Name should be null or empty on error", name == null || name.isEmpty());
    }

    @Test
    public void testResponseSuccessWithCodeField() {
        PluginEntity plugin = PluginEntity.builder()
                .source("https://test.com")
                .detailGetter(
                    "function execute(url) {" +
                    "  return Response.success({" +
                    "    name: 'Test Novel'," +
                    "    author: 'Test Author'" +
                    "  });" +
                    "}"
                )
                .build();

        DetailLoader loader = DetailLoader.with(plugin);
        var result = loader.load("https://test.com/test-novel");

        assertNotNull("Result should not be null", result);
        assertEquals("Name should match", "Test Novel", result.getName());
        assertEquals("Author should match", "Test Author", result.getAuthor());
    }

    @Test
    public void testResponseSuccessDualWithCodeField() {
        PluginEntity plugin = PluginEntity.builder()
                .source("https://test.com")
                .tocGetter(
                    "function execute(url) {" +
                    "  return Response.success([" +
                    "    {name: 'Chapter 1', url: '/chap-1'}," +
                    "    {name: 'Chapter 2', url: '/chap-2'}" +
                    "  ], 'next-page-token');" +
                    "}"
                )
                .build();

        ListLoader loader = ListLoader.with(plugin);
        var chapters = loader.load("https://test.com");

        assertNotNull("Chapters should not be null", chapters);
        assertEquals("Should extract 2 chapters", 2, chapters.size());
        assertEquals("First chapter name", "Chapter 1", chapters.get(0).getName());
        assertEquals("Second chapter name", "Chapter 2", chapters.get(1).getName());
    }

    @Test
    public void testResponseSuccessWithMultipleDataResponse() {
        // Test Response.success() with multiple data types
        PluginEntity plugin = PluginEntity.builder()
                .source("https://test.com")
                .detailGetter(
                    "function execute(url) {" +
                    "  return Response.success({" +
                    "    name: 'Multi-Field Novel'," +
                    "    author: 'Test Author'," +
                    "    description: 'A novel with many fields'," +
                    "    cover: 'https://example.com/cover.jpg'," +
                    "    tags: ['action', 'adventure']," +
                    "    metadata: {" +
                    "      chapters: 100," +
                    "      status: 'ongoing'," +
                    "      rating: 4.5" +
                    "    }" +
                    "  });" +
                    "}"
                )
                .build();

        DetailLoader loader = DetailLoader.with(plugin);
        var result = loader.load("https://test.com/multi-field-novel");

        assertNotNull("Result should not be null", result);
        assertEquals("Name should match", "Multi-Field Novel", result.getName());
        assertEquals("Author should match", "Test Author", result.getAuthor());
        assertEquals("Description should match", "A novel with many fields", result.getIntroduce());
        assertEquals("Cover should match", "https://example.com/cover.jpg", result.getCover());
    }

    @Test
    public void testBackwardCompatibilityDirectData() {
        // Test that plugins returning data directly still work
        PluginEntity plugin = PluginEntity.builder()
                .source("https://test.com")
                .detailGetter(
                    "function execute(url) {" +
                    "  return {" +
                    "    name: 'Direct Novel'," +
                    "    author: 'Direct Author'" +
                    "  };" +
                    "}"
                )
                .build();

        DetailLoader loader = DetailLoader.with(plugin);
        var result = loader.load("https://test.com/direct");

        assertNotNull("Result should not be null", result);
        assertEquals("Name should match", "Direct Novel", result.getName());
        assertEquals("Author should match", "Direct Author", result.getAuthor());
    }

    @Test
    public void testBackwardCompatibilityDualData() {
        // Test that plugins using old dual response format still work
        PluginEntity plugin = PluginEntity.builder()
                .source("https://test.com")
                .tocGetter(
                    "function execute(url) {" +
                    "  return {" +
                    "    data: [" +
                    "      {name: 'Old Chapter 1', url: '/old-1'}" +
                    "    ]," +
                    "    data2: 'next-token'" +
                    "  };" +
                    "}"
                )
                .build();

        ListLoader loader = ListLoader.with(plugin);
        var chapters = loader.load("https://test.com");

        assertNotNull("Chapters should not be null", chapters);
        assertEquals("Should extract 1 chapter", 1, chapters.size());
        assertEquals("Chapter name", "Old Chapter 1", chapters.get(0).getName());
    }

    @Test
    public void testTextLoaderErrorResponse() {
        PluginEntity plugin = PluginEntity.builder()
                .source("https://test.com")
                .chapGetter(
                    "function execute(url) {" +
                    "  return Response.error('Chapter not available');" +
                    "}"
                )
                .build();

        TextLoader loader = TextLoader.with(plugin);
        String result = loader.load("https://test.com/chap-1");

        assertEquals("Should return empty string on error", "", result);
    }

    @Test
    public void testTextLoaderSuccessWithCodeField() {
        PluginEntity plugin = PluginEntity.builder()
                .source("https://test.com")
                .chapGetter(
                    "function execute(url) {" +
                    "  return Response.success('Chapter content here...');" +
                    "}"
                )
                .build();

        TextLoader loader = TextLoader.with(plugin);
        String result = loader.load("https://test.com/chap-1");

        assertEquals("Should return content", "Chapter content here...", result);
    }
}
