package dark.leech.text.util;

import java.awt.*;

/**
 * Created by Long on 10/3/2016.
 */
public class ColorUtils {
    private ColorUtils() {
    }

    public static final Color THEME_COLOR = SettingUtils.THEME_COLOR;
    public static final Color STATUS_BAR = new Color(getValue(THEME_COLOR.getRed() - 20), getValue(THEME_COLOR.getGreen() - 20), getValue(THEME_COLOR.getBlue() - 15));
    public static final Color BUTTON_TEXT = THEME_COLOR;
    public static final Color BUTTON_CLICK = BUTTON_TEXT.brighter();

    private static int getValue(int i) {
        return (i > 0) ? i : 0;
    }
}
