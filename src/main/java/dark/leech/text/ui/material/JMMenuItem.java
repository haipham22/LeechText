package dark.leech.text.ui.material;

import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;

import javax.swing.*;
import java.awt.*;

public class JMMenuItem extends JMenuItem {
    private JLabel label;
    public JMMenuItem(String text) {
        label = new JLabel();
        label.setText(text);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(FontUtils.TEXT_NORMAL);
        setBackground(Color.white);
        setBorderPainted(false);
        setLayout(new GridLayout());
        setPreferredSize(new Dimension(90, 30));
        add(label);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.WHITE);
        label.setForeground(ColorUtils.THEME_COLOR);
        if (isArmed()) {
            g2.setColor(ColorUtils.THEME_COLOR);
            label.setForeground(Color.WHITE);
        }
        g2.fillRect(0, 0, getWidth(), getHeight());
    }

}
