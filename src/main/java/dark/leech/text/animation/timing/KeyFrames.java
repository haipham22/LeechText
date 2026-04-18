package dark.leech.text.animation.timing;

import java.util.ArrayList;
import java.util.List;

/** Stub implementation for KeyFrames class */
public class KeyFrames<T> {

    private final List<Frame<T>> frames;

    public KeyFrames(List<Frame<T>> frames) {
        this.frames = frames;
    }

    public List<Frame<T>> get() {
        return frames;
    }

    public T getInterpolatedValueAt(double fraction) {
        if (frames.isEmpty()) return null;

        // Simple implementation - return the last frame's value
        // In a real implementation, this would interpolate between frames
        int index = (int) (fraction * (frames.size() - 1));
        if (index >= frames.size()) index = frames.size() - 1;
        if (index < 0) index = 0;

        return frames.get(index).getValue();
    }

    public static class Builder<T> {
        private final List<Frame<T>> frames = new ArrayList<>();
        private T initialValue;

        public Builder() {
            // Default constructor
        }

        public Builder(T initialValue) {
            this.initialValue = initialValue;
        }

        public Builder<T> addFrame(T value) {
            frames.add(new Frame<>(value));
            return this;
        }

        @SafeVarargs
        public final Builder<T> addFrames(T... values) {
            for (T value : values) {
                frames.add(new Frame<>(value));
            }
            return this;
        }

        public KeyFrames<T> build() {
            return new KeyFrames<>(frames);
        }
    }

    public static class Frame<T> {
        private final T value;

        public Frame(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }
    }
}
