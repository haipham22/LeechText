package dark.leech.text.ui.material;

import dark.leech.text.image.GaussianBlurFilter;
import dark.leech.text.listeners.BlurListener;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.ui.Animation;
import dark.leech.text.ui.main.App;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.GraphicsUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;

public abstract class JMDialog extends JDialog implements BlurListener {
    private Point pointLocation;
    private BlurListener blurListener;
    protected Container container;
    private BufferedImage blurBuffer;
    private BufferedImage backBuffer;
    private float alpha = 1.0f;
    private ChangeListener changeListener;
    private Color borderColor;


    public JMDialog() {
        super(App.getMain());
        blurListener = App.getMain();
    }

    public JMDialog(JMDialog owner) {
        super(owner);
        blurListener = owner;
    }

    protected void onCreate() {
        container = getContentPane();
        container.setLayout(null);
        container.setBackground(Color.WHITE);
        setModal(true);
        setResizable(false);
        setUndecorated(true);
        if (GraphicsUtils.TRANSLUCENT_SUPPORT)
            setOpacity(0.0f);
        setTitle("LeechText");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/dark/leech/res/icon.png")));

    }


    public void open() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                blurListener.setBlur(true);
                setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 5, 5));
                pointLocation = AppUtils.getLocation();
                int X = pointLocation.x + 390 / 2 - getWidth() / 2;
                X = X < 10 ? 10 : X;
                X = (X + getWidth() > AppUtils.width) ? AppUtils.width - getWidth() - 10 : X;
                int Y = pointLocation.y + 600 / 2 - getHeight() / 2;
                Y = (Y + getHeight() > AppUtils.height) ? AppUtils.height - getHeight() - 10 : Y;
                Y = Y < 10 ? 10 : Y;
                setLocation(X, Y);
                Animation.fadeIn(JMDialog.this);
                setVisible(true);
            }
        }).start();


    }

    protected void runOnUiThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    public void close() {
        blurListener.setBlur(false);
        if (changeListener != null) changeListener.doChanger();
        Animation.fadeOut(this);
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
    }

    public void setChangeListener(ChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    public void setBlurListener(BlurListener blurListener) {
        this.blurListener = blurListener;
    }

    public void setBlur(boolean blur) {
        if (blur) createBlur();
        else blurBuffer = null;
        repaint();
    }

    private void createBlur() {
        JRootPane root = SwingUtilities.getRootPane(this);
        blurBuffer = GraphicsUtils.createCompatibleImage(getWidth(), getHeight());
        Graphics2D g2 = blurBuffer.createGraphics();
        root.paint(g2);
        g2.dispose();

        backBuffer = blurBuffer;
        blurBuffer = GraphicsUtils.createThumbnailFast(blurBuffer, getWidth() / 2);
        blurBuffer = new GaussianBlurFilter(3).filter(blurBuffer, null);
        RescaleOp op = new RescaleOp(0.8f, 0, null);
        blurBuffer = op.filter(blurBuffer, null);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2 = (Graphics2D) g.create();
        if (isVisible() && blurBuffer != null) {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(backBuffer, 0, 0, null);
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
            g2.drawImage(blurBuffer, 0, 0, getWidth(), getHeight(), null);
        }
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 5, 5);
        }
        g2.dispose();

    }
}
