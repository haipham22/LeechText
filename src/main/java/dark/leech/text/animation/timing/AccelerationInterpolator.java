package dark.leech.text.animation.timing;

/**
 * Stub implementation for missing AccelerationInterpolator class
 */
public class AccelerationInterpolator {
    private double acceleration;
    private double deceleration;
    
    public AccelerationInterpolator(double acceleration, double deceleration) {
        this.acceleration = acceleration;
        this.deceleration = deceleration;
    }
    
    public double interpolate(double fraction) {
        // Simple linear interpolation as fallback
        return fraction;
    }
}