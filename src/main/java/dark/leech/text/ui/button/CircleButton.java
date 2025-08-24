package dark.leech.text.ui.button;

import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Long on 9/30/2016.
 */
public class CircleButton extends JButton {
    public CircleButton(String text, float fs) {
        setText(text);
        setForeground(Color.WHITE);
        setFont(FontUtils.ICON_NORMAL.deriveFont(Font.BOLD, fs));
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setFocusable(false);
    }
    public CircleButton(String text) {
        setText(text);
        setForeground(Color.WHITE);
        setFont(FontUtils.ICON_NORMAL);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setFocusable(false);
    }

    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        Color bc = ColorUtils.BUTTON_CLICK;
        Color background = new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 100);
        ButtonUI styledButton = new ButtonUI(true);
        styledButton.setRolloverBackground(background);
        setUI(styledButton);
    }
}
