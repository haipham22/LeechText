package dark.leech.text.ui.setting;

import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.ui.SyntaxDialog;
import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.button.SelectButton;
import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ItemStyle extends JMPanel {
    private JLabel labelTitle;
    private JLabel labelInfo;
    private SelectButton btSelect;
    private CircleButton btEdit;
    private String name;
    private String style;
    private String text;

    public ItemStyle() {
        this("", "", false);
    }

    public ItemStyle(String name, String tip, boolean selected) {
        this.name = name;
        onCreate();
        setName(name);
        labelInfo.setText(tip);
        labelTitle.setText(name);
    }

    public boolean isSelected() {
        return btSelect.isSelected();
    }

    public void setSelected(boolean selected) {
        btSelect.setSelected(selected);
        btEdit.setVisible(selected);

    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    private void onCreate() {
        labelTitle = new JLabel();
        labelInfo = new JLabel();
        btSelect = new SelectButton();

        // ---- labelTitle ----
        labelTitle.setFont(FontUtils.TEXT_BOLD);
        add(labelTitle);
        labelTitle.setBounds(25, 5, 290, 30);

        // ---- labelInfo ----
        add(labelInfo);
        labelInfo.setFont(FontUtils.TEXT_THIN);
        labelInfo.setForeground(Color.GRAY);
        labelInfo.setBounds(25, 30, 290, 30);

        // ---- labelSelected ----
        add(btSelect);
        btSelect.setChangeListener(new ChangeListener() {
            @Override
            public void doChanger() {
                btEdit.setVisible(btSelect.isSelected());
            }
        });
        btSelect.setBounds(335, 15, 30, 30);

        // ---- labelEdit ----
        btEdit = new CircleButton(StringUtils.EDIT);
        btEdit.setForeground(ColorUtils.THEME_COLOR);
        btEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SyntaxDialog edit = new SyntaxDialog(name, text, style);
                edit.setChangeListener(new ChangeListener() {
                    @Override
                    public void doChanger() {
                        text = edit.getText();
                    }
                });
                edit.open();

            }
        });
        btEdit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        add(btEdit);
        btEdit.setBounds(300, 15, 30, 30);
        setPreferredSize(new Dimension(370, 60));
    }

}
