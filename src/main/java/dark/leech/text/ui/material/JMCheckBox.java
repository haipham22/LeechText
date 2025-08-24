package dark.leech.text.ui.material;

import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.ui.button.SelectButton;
import dark.leech.text.util.FontUtils;

import javax.swing.*;

/**
 * Created by Long on 9/10/2016.
 */
public class JMCheckBox extends JMPanel {
    private boolean TEXT_ON_RIGHT;
    private JLabel lbName;
    private String name;
    private SelectButton checkbox;

    public JMCheckBox(String name) {
        this.name = name;
        onCreate();
    }

    public JMCheckBox(String name, boolean TEXT_ON_RIGHT) {
        this.name = name;
        this.TEXT_ON_RIGHT = TEXT_ON_RIGHT;
        onCreate();
    }

    private void onCreate() {
        lbName = new JLabel(name);
        lbName.setFont(FontUtils.TEXT_NORMAL);
        add(lbName);
        checkbox = new SelectButton();
        checkbox.setFont(FontUtils.ICON_NORMAL);
        add(checkbox);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        if (TEXT_ON_RIGHT) {
            lbName.setBounds(height, 0, lbName.getPreferredSize().width, height);
            checkbox.setBounds(0, 0, height, height);
        } else {
            lbName.setBounds(0, 0, lbName.getPreferredSize().width, height);
            checkbox.setBounds(width - height, 0, height, height);
        }
    }

    public boolean isChecked() {
        return checkbox.isSelected();
    }

    public void setChecked(boolean checked) {
        checkbox.setSelected(checked);
    }

    public void setChangeListener(ChangeListener changeListener) {
        checkbox.setChangeListener(changeListener);
    }
}
