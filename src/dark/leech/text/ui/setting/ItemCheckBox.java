package dark.leech.text.ui.setting;

import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ItemCheckBox extends JMPanel {
    private JLabel lbName;
    private JLabel lbSelect;
    private boolean selected;
    private String name;

    public ItemCheckBox(String name) {
        this.name = name;
        onCreate();
        setSelected(false);

    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        lbSelect.setText(selected ? StringUtils.CHECK : StringUtils.CHECK_BOX_OUTLINE);
        this.selected = selected;
    }

    private void onCreate() {
        this.setBackground(Color.white);
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setSelected(!selected);
            }
        });
        this.setLayout(null);
        lbName = new JLabel(name);
        lbSelect = new JLabel();
        lbName.setFont(FontUtils.TEXT_BOLD);
        lbName.setBounds(25, 5, 310, 30);
        lbSelect.setForeground(ColorUtils.THEME_COLOR);
        lbSelect.setFont(FontUtils.ICON_NORMAL);
        lbSelect.setHorizontalAlignment(SwingConstants.CENTER);
        lbSelect.setBounds(335, 5, 30, 30);
        this.add(lbName);
        this.add(lbSelect);
        this.setPreferredSize(new Dimension(370, 40));
    }
}
