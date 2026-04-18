package dark.leech.text.animation.timing;

/** Stub implementation for TimingTarget interface */
public interface TimingTarget {

    /** Called when the animation begins */
    default void begin(Animator source) {
        // Default implementation
    }

    /** Called when the animation ends */
    default void end(Animator source) {
        // Default implementation
    }

    /** Called during animation timing events */
    default void timingEvent(Animator source, double fraction) {
        // Default implementation
    }

    /** Called when animation repeats */
    default void repeat(Animator source) {
        // Default implementation
    }
}
