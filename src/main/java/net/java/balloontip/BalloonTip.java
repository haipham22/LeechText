package net.java.balloontip;

import javax.swing.*;

/**
 * Stub implementation for BalloonTip class
 */
public class BalloonTip {
    
    public enum Orientation {
        LEFT_ABOVE, LEFT_BELOW, RIGHT_ABOVE, RIGHT_BELOW
    }
    
    public enum AttachLocation {
        ALIGNED, NORTHEAST, NORTHWEST, SOUTHEAST, SOUTHWEST
    }
    
    public BalloonTip(JComponent attachedComponent, String text) {
        // Stub implementation - could show as tooltip
        attachedComponent.setToolTipText(text);
    }
    
    public BalloonTip(JComponent attachedComponent, String text, Object style, boolean closeButton) {
        // Stub implementation
        attachedComponent.setToolTipText(text);
    }
    
    public BalloonTip(JComponent attachedComponent, JComponent contents, Object style, 
                     Orientation orientation, AttachLocation attachLocation, 
                     int horizontalOffset, int verticalOffset, boolean useCloseButton) {
        // Stub implementation
        attachedComponent.setToolTipText(contents.toString());
    }
    
    public void closeBalloon() {
        // Stub implementation
    }
    
    public void setVisible(boolean visible) {
        // Stub implementation
    }
}