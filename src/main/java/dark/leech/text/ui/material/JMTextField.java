package dark.leech.text.ui.material;

import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.SafePropertySetter;
import dark.leech.text.util.SettingUtils;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.BalloonTipStyle;
import net.java.balloontip.styles.MinimalBalloonStyle;
import net.java.balloontip.utils.TimingUtils;
import dark.leech.text.animation.timing.Animator;
import dark.leech.text.animation.timing.SwingTimerTimingSource;
import dark.leech.text.animation.timing.AccelerationInterpolator;
import dark.leech.text.animation.timing.SplineInterpolator;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

public class JMTextField extends JTextField {
    private Line line = new Line(this);

    public JMTextField() {
        setBorder(null);
        setCaret(new DefaultCaret() {
            @Override
            protected synchronized void damage(Rectangle r) {
                repaint();
            }
        });
        getCaret().setBlinkRate(500);
    }

    public void addError(String err) {
        Color color = ColorUtils.THEME_COLOR;
        BalloonTipStyle style = new MinimalBalloonStyle(new Color(color.getRed(), color.getGreen(), color.getBlue(), 200), 5);
        final BalloonTip balloonTip = new BalloonTip(
                this,
                new JLabel("<html><font color=\"#ffffff\">" + err + "</font></html>"),
                style,
                BalloonTip.Orientation.LEFT_ABOVE,
                BalloonTip.AttachLocation.ALIGNED,
                30, 10,
                false
        );
        TimingUtils.showTimedBalloon(balloonTip, 3000);
    }


    @Override
    public void setText(String s) {
        if (s != null)
            if (s.length() != 0)
                if (FontUtils.TEXT_NORMAL.canDisplayUpTo(s) == -1)
                    setFont(FontUtils.TEXT_NORMAL);
        super.setText(s);
        line.update();
    }

    @Override
    protected void processFocusEvent(FocusEvent e) {
        super.processFocusEvent(e);
        line.update();
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
        line.update();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        super.paintComponent(g);

        g2.setColor(Color.LIGHT_GRAY);
        g2.fillRect(0, getHeight() - 1, getWidth(), 1);

        g2.setColor(SettingUtils.THEME_COLOR);
        g2.fillRect((int) ((getWidth() - line.getWidth()) / 2), getHeight() - 3, (int) line.getWidth(), 2);
    }

    /**
     * An animated line below a text field.
     */
    public class Line {
        private final SwingTimerTimingSource timer;
        private final JComponent target;
        private Animator animator;
        private SafePropertySetter.Property<Double> width;

        Line(JComponent target) {
            this.target = target;
            this.timer = new SwingTimerTimingSource();
            timer.init();
            width = SafePropertySetter.animatableProperty(target, 0d);
        }

        void update() {
            if (animator != null) {
                animator.stop();
            }
            animator = new Animator.Builder(timer)
                    .setDuration(200, TimeUnit.MILLISECONDS)
                    .setEndBehavior(Animator.EndBehavior.HOLD)
                    .setInterpolator(new SplineInterpolator(0.4, 0, 0.2, 1))
                    .addTarget(SafePropertySetter.getTarget(width, width.getValue(), target.isFocusOwner() ? (double) target.getWidth() + 1 : 0d))
                    .build();
            animator.start();
        }

        public double getWidth() {
            return width.getValue();
        }
    }


}


