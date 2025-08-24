package dark.leech.text.ui.button;

import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Long on 10/20/2016.
 */
public class SelectButton extends JButton {
    private boolean selected;
    private ChangeListener changeListener;

    public SelectButton() {
        setSelected(selected);
        setBackground(Color.WHITE);
        setForeground(ColorUtils.BUTTON_TEXT);
        setFont(FontUtils.ICON_NORMAL);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        setFocusable(false);
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setSelected(!selected);
            }
        });
    }

    public void setSelected(boolean selected) {
        setText(selected ? StringUtils.CHECK_BOX : StringUtils.CHECK_BOX_OUTLINE);
        this.selected = selected;
        if (changeListener != null) changeListener.doChanger();
    }

    public boolean isSelected() {
        return selected;
    }

    public void setChangeListener(ChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        Color bc = ColorUtils.BUTTON_CLICK;
        Color background = new Color(bc.getRed(), bc.getGreen(), bc.getBlue(), 100);
        ButtonUI styledButton = new ButtonUI(true);
        styledButton.setRolloverBackground(background);
        setUI(styledButton);
    }
}
