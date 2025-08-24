package dark.leech.text.ui.button;

import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Created by Long on 9/30/2016.
 */
public class BasicButton extends JButton {
    public BasicButton() {
        setFont(FontUtils.TEXT_BOLD);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setBackground(Color.WHITE);
        setForeground(ColorUtils.BUTTON_TEXT);
        setFocusable(false);

    }


    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        ButtonUI btUi = new ButtonUI();
        btUi.setRolloverBackground(new Color(235, 235, 235));
        setUI(btUi);
    }


}
