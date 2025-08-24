package dark.leech.text.ui.material;

import dark.leech.text.animation.RippleEffect;
import dark.leech.text.util.SettingUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JMPanel extends JPanel {
    RippleEffect rippleEffect;

    public JMPanel(LayoutManager layoutManager) {
        super(layoutManager);
        setBackground(Color.WHITE);
        rippleEffect = RippleEffect.applyTo(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                repaint();
            }
        });
    }

    public JMPanel() {
        setLayout(null);
        setBackground(Color.WHITE);
        rippleEffect = RippleEffect.applyTo(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                repaint();
            }
        });
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(SettingUtils.THEME_COLOR);
        rippleEffect.paint(g2);
    }
}
