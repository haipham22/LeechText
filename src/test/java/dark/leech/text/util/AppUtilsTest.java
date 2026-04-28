package dark.leech.text.util;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Test;

public class AppUtilsTest {

    @Test
    public void shouldReturnAppHomeDirWhenProvided() throws Exception {
        String originalAppHomeDir = System.getProperty("app.home.dir");
        String originalUserDir = System.getProperty("user.dir");
        String originalUserHome = System.getProperty("user.home");

        try {
            System.setProperty("app.home.dir", "/custom/home");
            System.setProperty("user.dir", "/work/dir");
            System.setProperty("user.home", "/home/user");

            String result = invokeGetAppHomeDir();
            assertEquals("/custom/home", result);
        } finally {
            restoreProperty("app.home.dir", originalAppHomeDir);
            restoreProperty("user.dir", originalUserDir);
            restoreProperty("user.home", originalUserHome);
        }
    }

    @Test
    public void shouldReturnUserDirWhenAppHomeDirMissing() throws Exception {
        String originalAppHomeDir = System.getProperty("app.home.dir");
        String originalUserDir = System.getProperty("user.dir");
        String originalUserHome = System.getProperty("user.home");

        try {
            System.clearProperty("app.home.dir");
            System.setProperty("user.dir", "/work/dir");
            System.setProperty("user.home", "/home/user");

            String result = invokeGetAppHomeDir();
            assertEquals("/work/dir", result);
        } finally {
            restoreProperty("app.home.dir", originalAppHomeDir);
            restoreProperty("user.dir", originalUserDir);
            restoreProperty("user.home", originalUserHome);
        }
    }

    @Test
    public void shouldReturnUserHomeFallbackWhenAppHomeAndUserDirMissing() throws Exception {
        String originalAppHomeDir = System.getProperty("app.home.dir");
        String originalUserDir = System.getProperty("user.dir");
        String originalUserHome = System.getProperty("user.home");

        try {
            System.clearProperty("app.home.dir");
            System.clearProperty("user.dir");
            System.setProperty("user.home", "/home/user");

            String result = invokeGetAppHomeDir();
            assertEquals("/home/user/.leechtext", result);
        } finally {
            restoreProperty("app.home.dir", originalAppHomeDir);
            restoreProperty("user.dir", originalUserDir);
            restoreProperty("user.home", originalUserHome);
        }
    }

    @Test
    public void shouldIgnoreEmptyAppHomeDir() throws Exception {
        String originalAppHomeDir = System.getProperty("app.home.dir");
        String originalUserDir = System.getProperty("user.dir");
        String originalUserHome = System.getProperty("user.home");

        try {
            System.setProperty("app.home.dir", "");
            System.setProperty("user.dir", "/work/dir");
            System.setProperty("user.home", "/home/user");

            String result = invokeGetAppHomeDir();
            assertEquals("/work/dir", result);
        } finally {
            restoreProperty("app.home.dir", originalAppHomeDir);
            restoreProperty("user.dir", originalUserDir);
            restoreProperty("user.home", originalUserHome);
        }
    }

    private String invokeGetAppHomeDir() throws Exception {
        Method method = AppUtils.class.getDeclaredMethod("getAppHomeDir");
        method.setAccessible(true);
        return (String) method.invoke(null);
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
