package dark.leech.text.ui;

import dark.leech.text.ui.button.CloseButton;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Created by Long on 1/3/2017.
 */
public class PanelTitle extends JPanel {
    JLabel lbTitle = new JLabel();
    CloseButton btClose = new CloseButton();

    public void setText(String text) {
        lbTitle.setText(text);
    }

    public void addCloseListener(ActionListener actionListener) {
        btClose.addActionListener(actionListener);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        this.setBackground(ColorUtils.THEME_COLOR);
        this.setLayout(null);
        lbTitle.setFont(FontUtils.TITLE_NORMAL);
        lbTitle.setForeground(Color.WHITE);
        this.add(lbTitle);
        lbTitle.setBounds(20, 0, width - 60, height);
        this.add(btClose);
        btClose.setBounds(width - 35, 10, height - 10 * 2, height - 10 * 2);
    }
}
