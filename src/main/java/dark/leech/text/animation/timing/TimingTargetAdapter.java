package dark.leech.text.animation.timing;

/**
 * Stub implementation for missing TimingTargetAdapter class
 */
public abstract class TimingTargetAdapter {
    
    public void begin(Animator source) {
        // Default implementation - can be overridden
    }
    
    public void end(Animator source) {
        // Default implementation - can be overridden  
    }
    
    public void timingEvent(Animator source, double fraction) {
        // Default implementation - can be overridden
    }
    
    public void repeat(Animator source) {
        // Default implementation - can be overridden
    }
}