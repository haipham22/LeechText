package dark.leech.text.ui.material;

import dark.leech.text.util.ColorUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by Long on 9/23/2016.
 */
public class JMPopupMenu extends JPopupMenu {
    private static final int OFFSET = 4;
    private BufferedImage shadow;
    private Border border;

    @Override
    public boolean isOpaque() {
        return false;
    }

    @Override
    public void updateUI() {
        setBorder(null);
        super.updateUI();
        border = null;
        setBorder(new LineBorder(ColorUtils.THEME_COLOR.brighter()));
    }

    @Override
    protected void paintComponent(Graphics g) {
        //super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.drawImage(shadow, 0, 0, this);
        g2.setPaint(getBackground()); //??? 1.7.0_03
        g2.fillRect(0, 0, getWidth() - OFFSET, getHeight() - OFFSET);
        g2.dispose();
    }

    @Override
    public void show(Component c, int x, int y) {
        if (border == null) {
            Border inner = getBorder();
            Border outer = BorderFactory.createEmptyBorder(0, 0, OFFSET, OFFSET);
            border = BorderFactory.createCompoundBorder(outer, inner);
        }
        setBorder(border);
        Dimension d = getPreferredSize();
        int w = d.width;
        int h = d.height;
        if (shadow == null || shadow.getWidth() != w || shadow.getHeight() != h) {
            shadow = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = shadow.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f));
            g2.setPaint(Color.BLACK);
            for (int i = 0; i < OFFSET; i++) {
                g2.fillRoundRect(
                        OFFSET, OFFSET, w - OFFSET - OFFSET + i, h - OFFSET - OFFSET + i, 4, 4);
            }
            g2.dispose();
        }
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                Window pop = SwingUtilities.getWindowAncestor(JMPopupMenu.this);
                if (pop instanceof JWindow) {
                    pop.setBackground(new Color(0x0, true)); //JDK 1.7.0
                }
            }
        });
        super.show(c, x, y);
    }
}

