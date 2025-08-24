package dark.leech.text.ui.notification;

import dark.leech.text.util.AppUtils;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.GraphicsUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class Toast extends JWindow {
    private static int toastCount;
    private int timeShow = 2000;
    private String text;
    private Font fontText = FontUtils.TITLE_THIN;


    public static Toast Build() {
        return new Toast();
    }

    public Toast time(int timeShow) {
        this.timeShow = timeShow;
        return this;
    }

    public Toast content(String text) {
        this.text = text;
        return this;
    }

    public Toast font(Font fontText) {
        this.fontText = fontText;
        return this;
    }


    public void open() {
        if (toastCount != 0) {
            AppUtils.pause(1000);
            open();
        } else
            doOpen();
    }

    private void doOpen() {
        toastCount++;
        setLayout(new GridLayout());
        setAlwaysOnTop(true);
        getContentPane().setBackground(ColorUtils.THEME_COLOR);
        JLabel label = new JLabel("  " + text + "  ");
        // label.setPreferredSize(new Dimension(label.getPreferredSize().width, label.getPreferredSize().height + 6));
        label.setForeground(Color.white);
        label.setFont(fontText);
        getContentPane().add(label);
        pack();
        setShape(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 5, 5));
        setVisible(true);
        final int x = AppUtils.getX() + 390 / 2 - getWidth() / 2;
        final int y = AppUtils.getY() + 80;

        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    for (int i = 0; i < 10; i++) {
                        setLocation(x, y + i);
                        if (GraphicsUtils.TRANSLUCENT_SUPPORT)
                            setOpacity((float) i / 10);
                        AppUtils.pause(20);
                    }
                    AppUtils.pause(timeShow);
                    for (int i = 0; i < 10; i++) {
                        setLocation(x, y - i);
                        if (GraphicsUtils.TRANSLUCENT_SUPPORT)
                            setOpacity((float) (10 - i) / 10);
                        AppUtils.pause(20);
                    }
                } catch (Exception e) {
                }
                toastCount--;
                dispose();
            }

        }).start();

    }

    private Toast() {

    }


}
