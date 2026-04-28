package dark.leech.text.util;

import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileUtilsBootstrapTest {

    private String originalCurDir;
    private Path tempTestDir;

    @Before
    public void setUp() throws Exception {
        originalCurDir = AppUtils.curDir;
        tempTestDir = Files.createTempDirectory("leechtext-bootstrap-test");
    }

    @After
    public void tearDown() throws Exception {
        AppUtils.curDir = originalCurDir;
        if (tempTestDir != null && Files.exists(tempTestDir)) {
            Files.walk(tempTestDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(
                            path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (Exception ignored) {
                                }
                            });
        }
    }

    @Test
    public void shouldCreateHomeDirectoryWhenMissing() {
        Path homePath = tempTestDir.resolve(".leechtext/home");
        AppUtils.curDir = homePath.toString();

        FileUtils.bootstrapHomeDirectory();

        assertTrue(Files.exists(homePath));
        assertTrue(Files.isDirectory(homePath));
    }

    @Test
    public void shouldNotFailWhenHomeDirectoryAlreadyExists() throws Exception {
        Path homePath = tempTestDir.resolve(".leechtext/home");
        Files.createDirectories(homePath);
        AppUtils.curDir = homePath.toString();

        FileUtils.bootstrapHomeDirectory();

        assertTrue(Files.exists(homePath));
        assertTrue(Files.isDirectory(homePath));
    }
}
