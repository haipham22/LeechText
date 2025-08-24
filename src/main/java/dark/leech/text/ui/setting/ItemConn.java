package dark.leech.text.ui.setting;

import java.awt.*;

import javax.swing.*;

import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.ui.material.JMTextField;
import dark.leech.text.util.FontUtils;

public class ItemConn extends JMPanel {
    private final String title;
    private JMTextField tf;

    public ItemConn(String title, String text) {
        this.title = title;
        onCreate();
        setText(text);
    }

    private void onCreate() {
        this.setBackground(Color.white);
        this.setLayout(null);
        JLabel lbName = new JLabel(title);
        lbName.setFont(FontUtils.TEXT_BOLD);
        this.add(lbName);
        lbName.setBounds(25, 5, 250, 30);
        tf = new JMTextField();
        this.add(tf);
        tf.setBounds(300, 3, 60, 35);
        this.setPreferredSize(new Dimension(370, 40));
    }

    public String getText() {
        return tf.getText();
    }

    public void setText(String text) {
        tf.setText(text);
    }
}
