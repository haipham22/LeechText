package dark.leech.text.ui.notification;

import dark.leech.text.image.ImageLabel;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FontUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Long on 9/17/2016.
 */
public class Notification extends JWindow {
    public static int pointX;
    public static int pointY;
    private String name;
    private String contentNotification;
    private String imagePath;
    private InputStream imageStream;
    private int timeDelay = 5000;
    private boolean close = false;

    public static Notification build() {
        return new Notification();
    }

    public Notification title(String name) {
        this.name = name;
        return this;
    }

    public Notification content(String contentNotification) {
        this.contentNotification = contentNotification;
        return this;
    }

    public Notification path(String imagePath) {
        this.imagePath = imagePath;
        return this;
    }

    public Notification input(InputStream imageStream) {
        this.imageStream = imageStream;
        return this;
    }

    public Notification delay(int timeDelay) {
        this.timeDelay = timeDelay;
        return this;
    }


    private void onCreate() {

        setAlwaysOnTop(true);
        setSize(280, 90);
        ImageLabel lbCover = new ImageLabel();
        JLabel lbName = new JLabel();
        JLabel lbNoti = new JLabel();
        JLabel lbTime = new JLabel();
        Container panel1 = getContentPane();
        panel1.setBackground(Color.WHITE);
        panel1.setLayout(null);
        //---- lbCover ----
        lbCover.path(imagePath).input(imageStream).load();
        lbCover.setOpaque(true);
        panel1.add(lbCover);
        lbCover.setBounds(5, 5, 54, 81);

        //---- lbName ----
        lbName.setText(name);
        lbName.setFont(FontUtils.TEXT_NORMAL);
        panel1.add(lbName);
        lbName.setBounds(70, 5, 175, 30);

        //---- lbNoti ----
        lbNoti.setText(contentNotification);
        lbNoti.setFont(FontUtils.TEXT_NORMAL);
        panel1.add(lbNoti);
        lbNoti.setBounds(70, 35, 170, 30);

        //---- lbTime ----
        Date todaysDate = new Date();
        DateFormat df = new SimpleDateFormat("HH:mm:ss - dd/MM/yyyy");
        lbTime.setText(df.format(todaysDate));
        lbTime.setFont(FontUtils.TEXT_THIN);
        lbTime.setHorizontalAlignment(SwingConstants.RIGHT);
        panel1.add(lbTime);
        lbTime.setBounds(130, 65, 140, 20);
        getRootPane().setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
    }


    public void open() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                onCreate();
                setVisible(true);
                int width = AppUtils.width;
                int height = AppUtils.height;
                if (pointX == 0) pointX = width;
                if (pointY == 0) pointY = height - getHeight() - 50;
                final int x = pointX;
                final int y = pointY;
                pointY -= getHeight() + 5;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < getWidth() + 10; i++) {
                            setLocation(x - i, y);
                            AppUtils.pause(3);
                        }
                        AppUtils.pause(timeDelay);
                        if (!close) doClose();
                    }
                }).start();
            }
        });

    }

    private void doClose() {
        if (close) return;
        close = true;
        final int x = getLocation().x;
        final int y = getLocation().y;
        pointY = y;
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < getWidth() + 10; i++) {
                    setLocation(x + i, y);
                    AppUtils.pause(3);
                }
                dispose();
            }
        }).start();
    }

}
