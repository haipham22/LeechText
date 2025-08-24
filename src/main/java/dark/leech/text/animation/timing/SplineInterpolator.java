package dark.leech.text.animation.timing;

/** Stub implementation for SplineInterpolator class */
public class SplineInterpolator {
    private final double x1;
    private final double y1;
    private final double x2;
    private final double y2;

    public SplineInterpolator(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public double interpolate(double fraction) {
        // Simple linear interpolation as fallback
        return fraction;
    }
}
