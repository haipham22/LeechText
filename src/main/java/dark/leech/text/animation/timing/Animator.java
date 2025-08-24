package dark.leech.text.animation.timing;

import java.util.concurrent.TimeUnit;

/** Stub implementation for missing Animator class */
public class Animator {

    public Direction getCurrentDirection() {
        return Direction.FORWARD;
    }

    public void start() {
        // Stub implementation
    }

    public void stop() {
        // Stub implementation
    }

    public enum Direction {
        FORWARD,
        BACKWARD
    }

    public enum EndBehavior {
        HOLD,
        RESET
    }

    public static class Builder {
        private final SwingTimerTimingSource source;

        public Builder(SwingTimerTimingSource source) {
            this.source = source;
        }

        public Builder setDuration(int duration) {
            return this;
        }

        public Builder setDuration(int duration, TimeUnit unit) {
            // Convert to milliseconds and ignore for now
            return this;
        }

        public Builder setEndBehavior(EndBehavior behavior) {
            return this;
        }

        public Builder setInterpolator(Object interpolator) {
            return this;
        }

        public Builder addTarget(Object target) {
            return this;
        }

        public Animator build() {
            return new Animator();
        }
    }
}
