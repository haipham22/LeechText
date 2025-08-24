package dark.leech.text.util;

import java.awt.*;
import java.util.concurrent.atomic.AtomicReference;

import dark.leech.text.animation.timing.Animator;
import dark.leech.text.animation.timing.KeyFrames;
import dark.leech.text.animation.timing.TimingTarget;
import dark.leech.text.animation.timing.TimingTargetAdapter;

/** Created by Long on 1/5/2017. */
public class SafePropertySetter<T> extends TimingTargetAdapter implements TimingTarget {
    private final AtomicReference<KeyFrames<T>> keyFrames;
    private final boolean isToAnimation;
    private final SafePropertySetter.Getter<T> getter;
    private final SafePropertySetter.Setter<T> setter;

    protected SafePropertySetter(
            KeyFrames<T> keyFrames,
            boolean isToAnimation,
            SafePropertySetter.Getter<T> getter,
            SafePropertySetter.Setter<T> setter) {
        this.keyFrames = new AtomicReference<>(keyFrames);
        this.isToAnimation = isToAnimation;
        this.getter = getter;
        this.setter = setter;
    }

    @SafeVarargs
    public static <T> TimingTarget getTarget(SafePropertySetter.Setter<T> setter, T... values) {
        return new SafePropertySetter<>(
                new KeyFrames.Builder<T>().addFrames(values).build(), false, null, setter);
    }

    public static <T> TimingTarget getTarget(
            SafePropertySetter.Setter<T> setter, KeyFrames<T> keyFrames) {
        return new SafePropertySetter<>(keyFrames, false, null, setter);
    }

    @SafeVarargs
    public static <T> TimingTarget getTargetTo(
            SafePropertySetter.Getter<T> getter, SafePropertySetter.Setter<T> setter, T... values) {
        return getTargetTo(
                getter, setter, new KeyFrames.Builder<T>(values[0]).addFrames(values).build());
    }

    @SafeVarargs
    public static <T> TimingTarget getTargetTo(
            SafePropertySetter.GetterAndSetter<T> getterAndSetter, T... values) {
        return getTargetTo(getterAndSetter, getterAndSetter, values);
    }

    public static <T> TimingTarget getTargetTo(
            SafePropertySetter.Getter<T> getter,
            SafePropertySetter.Setter<T> setter,
            KeyFrames<T> keyFrames) {
        return new SafePropertySetter<>(keyFrames, true, getter, setter);
    }

    public static <T> TimingTarget getTargetTo(
            SafePropertySetter.GetterAndSetter<T> getterAndSetter, KeyFrames<T> keyFrames) {
        return getTargetTo(getterAndSetter, getterAndSetter, keyFrames);
    }

    public static <U> SafePropertySetter.Property<U> animatableProperty(
            Component component, U value) {
        return new SafePropertySetter.Property<>(component, value);
    }

    public void timingEvent(Animator source, double fraction) {
        setter.setValue(this.keyFrames.get().getInterpolatedValueAt(fraction));
    }

    public void begin(Animator source) {
        if (isToAnimation) {
            KeyFrames.Builder<T> builder = new KeyFrames.Builder<>();
            boolean first = true;
            for (KeyFrames.Frame<T> frame : keyFrames.get().get()) {
                if (first) {
                    first = false;
                } else {
                    builder.addFrame(frame.getValue());
                }
            }
            keyFrames.set(builder.build());
        }

        double fraction = source.getCurrentDirection() == Animator.Direction.FORWARD ? 0.0D : 1.0D;
        this.timingEvent(source, fraction);
    }

    public interface Getter<T> {
        T getValue();
    }

    public interface Setter<T> {
        void setValue(T value);
    }

    public interface GetterAndSetter<T>
            extends SafePropertySetter.Getter<T>, SafePropertySetter.Setter<T> {}

    public static class Property<T> implements SafePropertySetter.GetterAndSetter<T> {
        private final Component component;
        private T value;

        public Property(Component component, T value) {
            this.component = component;
            this.value = value;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public void setValue(T newValue) {
            value = newValue;
            if (component != null) {
                component.repaint();
            }
        }
    }
}
