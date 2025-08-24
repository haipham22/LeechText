package dark.leech.text.animation;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

import dark.leech.text.animation.timing.AccelerationInterpolator;
import dark.leech.text.animation.timing.Animator;
import dark.leech.text.animation.timing.SwingTimerTimingSource;
import dark.leech.text.animation.timing.TimingTargetAdapter;
import dark.leech.text.util.SafePropertySetter;

/** A ripple effect. */
public class RippleEffect {
    private final List<RippleAnimation> ripples = new ArrayList<>();
    private final JComponent target;
    private final SwingTimerTimingSource timer;

    private RippleEffect(final JComponent component) {
        this.target = component;

        timer = new SwingTimerTimingSource();
        timer.init();
    }

    /**
     * Creates a ripple effect for the given component. You need to call {@link #paint(Graphics)} in
     * your drawing method to actually paint this effect.
     *
     * @param target target component
     * @return ripple effect for that component
     */
    public static RippleEffect applyTo(final JComponent target) {
        final RippleEffect rippleEffect = new RippleEffect(target);
        target.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        rippleEffect.addRipple(e.getPoint(), target.getWidth());
                    }
                });
        return rippleEffect;
    }

    /**
     * Creates a ripple effect for the given component that is limited to the component's size and
     * will always start in the center. You need to call {@link #paint(Graphics)} in your drawing
     * method to actually paint this effect.
     *
     * @param target target component
     * @return ripple effect for that component
     */
    public static RippleEffect applyFixedTo(final JComponent target) {
        final RippleEffect rippleEffect = new RippleEffect(target);
        target.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        rippleEffect.addRipple(new Point(24, 24), target.getWidth() / 2);
                    }
                });
        return rippleEffect;
    }

    /**
     * Paints this effect.
     *
     * @param g canvas
     */
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        for (RippleAnimation rippleAnimation : ripples) {
            float rippleOpacity = rippleAnimation.rippleOpacity.getValue().floatValue();
            Point rippleCenter = rippleAnimation.rippleCenter;
            int rippleRadius = rippleAnimation.rippleRadius.getValue();

            Color fg = target.getForeground();
            g2.setColor(
                    new Color(
                            fg.getRed() / 255f,
                            fg.getGreen() / 255f,
                            fg.getBlue() / 255f,
                            rippleOpacity));
            g2.fillOval(
                    rippleCenter.x - rippleRadius,
                    rippleCenter.y - rippleRadius,
                    2 * rippleRadius,
                    2 * rippleRadius);
        }
    }

    /**
     * Adds a ripple at the given point.
     *
     * @param point point to add the ripple at
     * @param maxRadius the maximum radius of the ripple
     */
    public void addRipple(Point point, int maxRadius) {
        final RippleAnimation ripple = new RippleAnimation(point, maxRadius);
        ripples.add(ripple);
        ripple.start();
    }

    /** A ripple animation (one ripple circle after one click). */
    public class RippleAnimation {
        private final Point rippleCenter;
        private final int maxRadius;
        private final SafePropertySetter.Property<Integer> rippleRadius =
                SafePropertySetter.animatableProperty(target, 25);
        private final SafePropertySetter.Property<Double> rippleOpacity =
                SafePropertySetter.animatableProperty(target, 0.0);

        private RippleAnimation(Point rippleCenter, int maxRadius) {
            this.rippleCenter = rippleCenter;
            this.maxRadius = maxRadius;
        }

        void start() {
            // rippleCenter.setLocation(rippleCenter);
            Animator rippleAnimator =
                    new Animator.Builder(timer)
                            .setDuration(1000, TimeUnit.MILLISECONDS)
                            .setEndBehavior(Animator.EndBehavior.HOLD)
                            .setInterpolator(new AccelerationInterpolator(0.2, 0.19))
                            .addTarget(
                                    SafePropertySetter.getTarget(
                                            rippleRadius, 0, maxRadius / 2, maxRadius, maxRadius))
                            .addTarget(
                                    SafePropertySetter.getTarget(rippleOpacity, 0.0, 0.4, 0.3, 0.0))
                            .addTarget(
                                    new TimingTargetAdapter() {
                                        @Override
                                        public void end(Animator source) {
                                            ripples.remove(RippleAnimation.this);
                                        }
                                    })
                            .build();
            rippleAnimator.start();
        }
    }
}
