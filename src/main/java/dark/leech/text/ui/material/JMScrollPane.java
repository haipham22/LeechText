package dark.leech.text.ui.material;

import dark.leech.text.util.ColorUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;


public class JMScrollPane extends JScrollPane {
    public JMScrollPane(Component view) {
        super(view);
        JScrollBar sb = getVerticalScrollBar();
        sb.setUI(new JMScrollBar());
        sb.setBackground(Color.WHITE);
        sb.setPreferredSize(new Dimension(10, 0));
        setBorder(null);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        getVerticalScrollBar().setUnitIncrement(20);
    }

    public JMScrollPane() {
        JScrollBar sb = getVerticalScrollBar();
        sb.setUI(new JMScrollBar());
        sb.setBackground(Color.WHITE);
        sb.setPreferredSize(new Dimension(10, 0));
        setBorder(new LineBorder(ColorUtils.THEME_COLOR, 0));
        getVerticalScrollBar().setUnitIncrement(20);
    }
}

class JMScrollBar extends BasicScrollBarUI {
    private static final int SCROLL_BAR_ALPHA_ROLLOVER = 150;
    private static final int SCROLL_BAR_ALPHA = 100;
    private static final int THUMB_BORDER_SIZE = 2;
    private static final int THUMB_SIZE = 8;
    private static final Color THUMB_COLOR = ColorUtils.THEME_COLOR;
    private final JButton b = new JButton() {

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(0, 0);
        }

    };

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        int alpha = isThumbRollover() ? SCROLL_BAR_ALPHA_ROLLOVER : SCROLL_BAR_ALPHA;
        int orientation = scrollbar.getOrientation();
        int x = thumbBounds.x + THUMB_BORDER_SIZE;
        int y = thumbBounds.y + THUMB_BORDER_SIZE;

        int width = orientation == JScrollBar.VERTICAL ? THUMB_SIZE : thumbBounds.width - (THUMB_BORDER_SIZE * 2);
        width = Math.max(width, THUMB_SIZE);

        int height = orientation == JScrollBar.VERTICAL ? thumbBounds.height - (THUMB_BORDER_SIZE * 2) : THUMB_SIZE;
        height = Math.max(height, THUMB_SIZE);


        Graphics2D graphics2D = (Graphics2D) g.create();
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setColor(new Color(THUMB_COLOR.getRed(), THUMB_COLOR.getGreen(), THUMB_COLOR.getBlue(), alpha));
        graphics2D.fillRect(x, y, width, height);
        graphics2D.dispose();
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return b;
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return b;
    }
}