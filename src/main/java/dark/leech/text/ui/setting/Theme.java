package dark.leech.text.ui.setting;

import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.util.FontUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Created by Long on 10/7/2016.
 */
public class Theme extends JMPanel {
    private Color color;
    private JPanel colorPn;

    public Theme(Color color) {
        this.color = color;
        onCreate();
    }

    private void onCreate() {
        setBackground(Color.white);
        setLayout(null);
        JLabel labelName = new JLabel("Chủ đề");
        labelName.setFont(FontUtils.TEXT_BOLD);
        add(labelName);
        labelName.setBounds(25, 5, 250, 30);
        colorPn = new JPanel();
        colorPn.setBackground(color);
        colorPn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        colorPn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                clickChooserColor();
            }
        });
        add(colorPn);
        colorPn.setBounds(335, 5, 30, 30);
        setPreferredSize(new Dimension(370, 40));
    }

    private void clickChooserColor() {
        final ChooserColor chooserColor = new ChooserColor(color);

        chooserColor.setChangeListener(new ChangeListener() {
            @Override
            public void doChanger() {
                color = chooserColor.getChooserColor();
                colorPn.setBackground(color);
            }
        });
        chooserColor.open();

    }

    public void setThemeColor(Color color) {
        colorPn.setBackground(color);
        this.color = color;
    }

    public Color getColor() {
        return color;
    }
}

class ChooserColor extends JMDialog {
    private static final Color DARK_THEME = new Color(38, 50, 56);
    private static final Color RED_THEME = new Color(244, 67, 54);
    private static final Color INDIGO_THEME = new Color(63, 81, 181);
    private static final Color BLUE_THEME = new Color(33, 150, 243);
    private static final Color PURPLE_THEME = new Color(156, 39, 176);
    private static final Color DEEP_PURPLE_THEME = new Color(103, 58, 183);
    private static final Color TEAL_THEME = new Color(0, 150, 136);
    private static final Color GREEN_THEME = new Color(76, 175, 80);
    private static final Color BROWN_THEME = new Color(121, 85, 72);
    private Color color;

    public ChooserColor(Color color) {
        this.color = color;
        onCreate();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        container.setLayout(new FlowLayout());
        container.add(new ColorPane(DARK_THEME));
        container.add(new ColorPane(RED_THEME));
        container.add(new ColorPane(INDIGO_THEME));
        container.add(new ColorPane(BLUE_THEME));
        container.add(new ColorPane(PURPLE_THEME));
        container.add(new ColorPane(DEEP_PURPLE_THEME));
        container.add(new ColorPane(TEAL_THEME));
        container.add(new ColorPane(GREEN_THEME));
        container.add(new ColorPane(BROWN_THEME));
        setSize(160, 160);
    }

    public Color getChooserColor() {
        return color;
    }

    public class ColorPane extends JPanel {
        public ColorPane(Color color) {
            setPreferredSize(new Dimension(45, 45));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    choose();
                }
            });
            setBackground(color);
        }

        private void choose() {
            color = getBackground();
            close();
        }
    }
}
