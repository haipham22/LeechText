package dark.leech.text.ui.notification;

import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.button.CloseButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FontUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Created by Long on 9/20/2016.
 */
public class Alert extends JMDialog {
    private CloseButton labelClose;
    private JLabel labelTitle;
    private BasicButton buttonOk;
    private JTextPane textPane;
    private String text;

    private Alert(String text) {
        this.text = text;
        setAlwaysOnTop(true);
        setModal(true);
        setUndecorated(true);
        onCreate();
        open();
    }

    public static Alert show(String text) {
        return new Alert(text);
    }

    protected void onCreate() {
        super.onCreate();
        Container panel = getContentPane();
        labelTitle = new JLabel();
        buttonOk = new BasicButton();
        textPane = new JTextPane();

        panel.setLayout(null);
        panel.setBackground(Color.WHITE);

        //---- labelClose ----
        labelClose = new CloseButton();
        labelClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        panel.add(labelClose);
        labelClose.setBounds(290, 5, 25, 25);

        //---- labelTitle ----
        labelTitle.setText("Có lỗi xảy ra!");
        labelTitle.setFont(FontUtils.TEXT_BOLD);
        panel.add(labelTitle);
        labelTitle.setBounds(15, 0, 245, 35);

        //---- buttonOk ----
        buttonOk.setText("OK");
        buttonOk.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispose();
            }
        });
        panel.add(buttonOk);
        buttonOk.setBounds(10, 110, 280, 35);

        //---- text ----
        textPane.setText(text);
        textPane.setBorder(null);
        textPane.setBackground(Color.WHITE);
        textPane.setEditable(false);
        textPane.setFont(FontUtils.TEXT_NORMAL);
        panel.add(textPane);
        textPane.setBounds(20, 35, 285, 65);
        panel.setPreferredSize(new Dimension(320, 155));
        getRootPane().setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
        pack();
        Point pointLocation = AppUtils.getLocation();
        setLocation(pointLocation.x + 390 / 2 - getWidth() / 2, pointLocation.y + 600 / 2 - getHeight() / 2);
    }
}
