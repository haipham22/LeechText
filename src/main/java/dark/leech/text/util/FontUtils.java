package dark.leech.text.util;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class FontUtils {
private FontUtils(){}
    public static Font iconFont(float size) {
        try {
            InputStream in = FontUtils.class.getResourceAsStream("/dark/leech/res/font/icon.ttf");
            return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(size);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Font titleFont(float size) {
        try {
            InputStream in = FontUtils.class.getResourceAsStream("/dark/leech/res/font/title.ttf");
            return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(size);
        } catch (IOException | FontFormatException e) {
        }
        return null;

    }

    public static Font textFont(float size, int type) {
        try {
            InputStream in = FontUtils.class.getResourceAsStream(
                    "/dark/leech/res/font/" + ((type == Font.BOLD) ? "textbold.ttf" : "textregular.ttf"));
            return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(size);
        } catch (IOException | FontFormatException e) {
        }
        return null;
    }

    public static Font codeFont(float size) {
        try {
            InputStream in = FontUtils.class.getResourceAsStream("/dark/leech/res/font/code.ttf");
            return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(size);
        } catch (IOException | FontFormatException e) {

        }
        return null;
    }
    public static final Font TITLE_BIG = titleFont(30f);
    public static final Font TITLE_NORMAL = titleFont(24f);
    public static final Font TITLE_THIN = titleFont(20f);
    public static final Font ICON_NORMAL = iconFont(23f);
    public static final Font TEXT_NORMAL = textFont(14f, Font.PLAIN);
    public static final Font TEXT_BOLD = textFont(16f, Font.BOLD);
    public static final Font TEXT_THIN = textFont(12f, Font.PLAIN);
}
