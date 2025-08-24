package dark.leech.text.ui.setting.trash;

import dark.leech.text.listeners.BlurListener;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.listeners.RemoveListener;
import dark.leech.text.models.Trash;
import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.button.SelectButton;
import dark.leech.text.ui.material.DropShadowBorder;
import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.SettingUtils;
import dark.leech.text.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Dark on 2/13/2017.
 */
class TrashItem extends JMPanel {
    private JLabel labelName;
    private CircleButton buttonEdit;
    private CircleButton buttonDelete;
    private SelectButton btSelect;
    private Trash trash;
    private RemoveListener removeListener;
    private BlurListener blurListener;

    public TrashItem(Trash trash) {
        this.trash = trash;
        gui();
    }

    private void gui() {
        setBackground(Color.white);
        setLayout(null);
        labelName = new JLabel();
        btSelect = new SelectButton();

        labelName.setText(trash.getTip());
        labelName.setFont(FontUtils.TEXT_NORMAL);
        add(labelName);
        labelName.setBounds(10, 5, 180, 30);

        buttonEdit = new CircleButton(StringUtils.EDIT);
        buttonEdit.setForeground(ColorUtils.THEME_COLOR);
        buttonEdit.setToolTipText("Sửa");

        buttonEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doEdit();
            }
        });
        add(buttonEdit);
        buttonEdit.setBounds(220, 5, 30, 30);

        btSelect.setSelected(trash.isReplace());
        add(btSelect);
        btSelect.setBounds(250, 5, 30, 30);

        buttonDelete = new CircleButton(StringUtils.DELETE);
        buttonDelete.setForeground(ColorUtils.THEME_COLOR);
        buttonDelete.setToolTipText("Xóa");
        buttonDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionDelete();
            }
        });
        add(buttonDelete);
        buttonDelete.setBounds(280, 5, 30, 30);

        setBorder(new DropShadowBorder(SettingUtils.THEME_COLOR, 5, 3));
        setPreferredSize(new Dimension(300, 45));
    }

    public void addRemoveListener(RemoveListener removeListener) {
        this.removeListener = removeListener;
    }

    public void addBlurListener(BlurListener blurListener) {
        this.blurListener = blurListener;
    }

    public Trash getTrash() {
        trash.setReplace(btSelect.isSelected());
        return trash;
    }

    private void actionDelete() {
        removeListener.removeComponent(this);
    }

    private void doEdit() {
        final TrashItemIDialog tiDialog = new TrashItemIDialog(trash);
        tiDialog.setBlurListener(blurListener);
        tiDialog.setChangeListener(new ChangeListener() {
            @Override
            public void doChanger() {
                trash = tiDialog.getTrash();
                labelName.setText(trash.getTip());
            }
        });
        tiDialog.open();


    }

}
