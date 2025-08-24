package dark.leech.text.ui.button;

import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.StringUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Long on 9/30/2016.
 */
public class CloseButton extends JButton {
    public CloseButton() {
        setText(StringUtils.CLOSE);
        setForeground(Color.WHITE);
        setFont(FontUtils.ICON_NORMAL);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setFocusable(false);
    }

    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        ButtonUI styledButton = new ButtonUI(true);
        Color bc = ColorUtils.BUTTON_CLICK;
        Color rolloverBackground = new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 200);
        styledButton.setRolloverBackground(rolloverBackground);
        styledButton.setPressedBackground(rolloverBackground);
        setUI(styledButton);
    }

}
