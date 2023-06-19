package dark.leech.text.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Created by Long on 1/10/2017.
 */
public class FileUtils {
    private FileUtils() {
    }

    public static void init() {
        mkdir(validate(AppUtils.curDir + "/tools/plugins"));
    }

    //Tao thu muc
    public static void mkdir(String dir) {
        File file = new File(validate(dir));
        if (file.exists())
            if (file.isDirectory())
                return;
        String[] path = dir.split(Pattern.quote(AppUtils.SEPARATOR));
        if (path.length == 1)
            return;
        dir = path[0];
        for (int i = 1; i < path.length; i++) {
            dir += AppUtils.SEPARATOR + path[i];
            file = new File(dir);
            if (!file.exists())
                file.mkdir();
        }
    }

    public static String file2string(String path, String charset) {
        try {
            return new String(file2byte(path), charset);
        } catch (Exception e) {
            return null;
        }
    }

    public static String file2string(String path) {
        return file2string(path, "UTF-8");
    }

    public static byte[] stream2byte(InputStream in) {
        try {
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
            }
        }
    }

    public static String stream2string(String path) {
        InputStream in = null;
        try {
            in = FileUtils.class.getResourceAsStream(path);
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            return new String(bytes, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
            }
        }
    }

    public static byte[] file2byte(File file) {
        FileInputStream fi = null;
        try {
            if (!file.exists())
                return null;
            fi = new FileInputStream(file);
            byte[] b = new byte[(int) file.length()];
            fi.read(b);
            return b;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fi != null) fi.close();
            } catch (IOException e) {
            }
        }
    }

    public static byte[] file2byte(String path) {
        return file2byte(new File(validate(path)));
    }

    public static void string2file(String source, String savepath) {
        string2file(source, savepath, "UTF-8");
    }

    public static void string2file(String src, String svp, String charset) {
        try {
            byte2file(src.getBytes(charset), svp);
        } catch (Exception e) {
        }
    }

    public static synchronized void byte2file(byte[] source, String savepath) {
        FileOutputStream fo = null;
        try {
            File f = new File(validate(savepath));
            if (!f.exists())
                f.createNewFile();
            fo = new FileOutputStream(f);
            fo.write(source);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fo != null) fo.close();
            } catch (IOException e) {
            }
        }
    }

    public static void add2file(String str, File file, String charset) {
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(file, true);
            fo.write(str.getBytes(charset));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fo != null) fo.close();
            } catch (IOException e) {
            }
        }
    }

    public static void add2file(String str, File file) {
        add2file(str, file, "UTF-8");
    }

    public static void add2file(File from, File src) {
        FileOutputStream fo = null;
        try {
            fo = new FileOutputStream(src, true);
            fo.write("\n".getBytes());
            fo.write(file2byte(from));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fo != null) fo.close();
            } catch (IOException e) {
            }
        }
    }

    public static void copyFile(String from, String to) {
        byte2file(file2byte(from), to);
    }

    public static void cutFile(String from, String to) {
        copyFile(from, to);
        deleteFile(from);
    }

    public static void url2file(String url, String savePath) {
        try {
            byte2file(Http.connect(url)
                    .execute()
                    .bodyAsBytes(), savePath);
        } catch (IOException e) {
        }
    }

    public static void deleteFile(String path) {
        try {
            File f = new File(validate(path));
            if (f.exists())
                f.delete();
        } catch (Exception e) {
        }
    }

    public static synchronized String validate(String path) {
        return path.replace("/", AppUtils.SEPARATOR);
    }

    public static String input2string(InputStream inputStream) {
        String result = null;
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            for (int length; (length = inputStream.read(buffer)) != -1; ) {
                baos.write(buffer, 0, length);
            }
            // StandardCharsets.UTF_8.name() > JDK 7
            result = baos.toString(StandardCharsets.UTF_8.toString());
        } catch (Exception ignore) {

        } finally {
            try {
                if (baos != null) baos.close();
            } catch (Exception e) {
            }
        }
        return result;
    }
}
