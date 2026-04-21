package dark.leech.text.action.export;

import dark.leech.text.models.Chapter;
import dark.leech.text.models.Properties;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import dark.leech.text.util.SettingUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ToCTest {

    @Test
    public void testNavPointClosingTags() throws Exception {
        Path tempDir = Files.createTempDirectory("leech-text-test-");
        
        // Create required data directories that caller is expected to provide
        new File(tempDir.toFile(), "data").mkdirs();
        new File(tempDir.toFile(), "data/Text").mkdirs();
        new File(tempDir.toFile(), "data/Images").mkdirs();
        
        // Initialize HTML_SYNTAX for tests because Settings dependencies may not load automatically
        SettingUtils.HTML_SYNTAX = "<html><head><title>Test</title></head><body></body></html>";
        
        Properties properties = new Properties();
        properties.setName("Test Book");
        properties.setAuthor("Test Author");
        properties.setSavePath(tempDir.toAbsolutePath().toString());
        properties.setAddGt(true); // Include Introduction to test its tags too
        
        List<Chapter> chapters = new ArrayList<>();
        chapters.add(new Chapter("url1", 1, "", "Chương 1: Bắt đầu"));
        chapters.add(new Chapter("url2", 2, "", "Chương 2: Tiếp tục"));
        properties.setChapList(chapters);
        
        ToC toc = new ToC(properties);
        toc.setAutoSplit(true);
        toc.setIncludeImg(false);
        
        // Generate ToC
        toc.mkToC();
        
        // Validate generated ncx file
        File ncxFile = new File(tempDir.toFile(), "data/toc.ncx");
        assertTrue("toc.ncx should be created", ncxFile.exists());
        
        String ncxContent = Files.readString(ncxFile.toPath());
        
        // We expect exact closing tags for navPoint 
        // We just verify that the number of opening <navPoint> matches </navPoint>
        
        int openCount = ncxContent.split("<navPoint").length - 1;
        int closeCount = ncxContent.split("</navPoint>").length - 1;
        
        assertEquals("All <navPoint> elements must be properly closed", openCount, closeCount);
    }
}
