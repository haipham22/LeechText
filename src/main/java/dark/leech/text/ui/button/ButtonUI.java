package dark.leech.text.ui.button;

import java.awt.*;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;

import lombok.Setter;

import dark.leech.text.util.ColorUtils;

/** Created by Long on 9/30/2016. */
class ButtonUI extends BasicButtonUI {
    private final boolean round;
    private final Color defaultBackground;

    @Setter private Color rolloverBackground;
    @Setter private Color pressedBackground;

    private Dimension size;

    public ButtonUI() {
        this(false);
    }

    public ButtonUI(boolean round) {
        Color bc = ColorUtils.BUTTON_CLICK;
        rolloverBackground = new Color(bc.getRed(), bc.getGreen(), bc.getBlue());
        pressedBackground = new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 100);
        defaultBackground = new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 0);
        this.round = round;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        AbstractButton button = (AbstractButton) c;
        size = c.getSize();
        button.setOpaque(false);
        button.setBorder(null);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        paintBackground(
                g,
                b,
                b.getModel().isRollover(),
                b.getModel().isPressed(),
                b.getModel().isSelected());
        super.paint(g, c);
    }

    private void paintBackground(
            Graphics g, JComponent c, boolean rollover, boolean pressed, boolean clicked) {
        Graphics2D g2 = (Graphics2D) g;
        RenderingHints rh =
                new RenderingHints(
                        RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHints(rh);
        g2.setColor(defaultBackground);
        if (rollover) g2.setColor(rolloverBackground);
        if (clicked || pressed) g2.setColor(pressedBackground);
        if (round) g.fillOval(size.width / 2 - size.height / 2, 0, size.height, size.height);
        else g2.fillRoundRect(0, 0, size.width, size.height, 5, 5);
    }
}
