package dark.leech.text.ui.setting.trash;

import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.models.Trash;
import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class TrashPane extends JMPanel {

    private static final long serialVersionUID = 1L;
    private List<Trash> trash;
    private CircleButton buttonEdit;

    public TrashPane() {
        this(null);
    }

    public TrashPane(List<Trash> trash) {
        this.trash = trash;
        JLabel labelName = new JLabel();
        JLabel labelTip = new JLabel();

        labelName.setText("Lọc rác");
        labelName.setFont(FontUtils.TEXT_BOLD);
        labelName.setBounds(25, 5, 290, 30);
        add(labelName);

        labelTip.setText("Tùy chỉnh lọc rác khi gettext");
        labelTip.setFont(FontUtils.TEXT_THIN);
        labelTip.setForeground(Color.GRAY);
        labelTip.setBounds(25, 30, 290, 30);
        add(labelTip);

        buttonEdit = new CircleButton(StringUtils.EDIT);
        buttonEdit.setForeground(ColorUtils.THEME_COLOR);
        buttonEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionEdit();
            }
        });
        add(buttonEdit);
        buttonEdit.setBounds(335, 15, 30, 30);
        setBackground(Color.white);
        setPreferredSize(new Dimension(370, 60));
        setLayout(null);
    }

    public List<Trash> getTrash() {
        return trash;
    }

    public void setTrash(List<Trash> trash) {
        this.trash = trash;
    }

    private void actionEdit() {
        final TrashUI trashUI = new TrashUI(trash);
        trashUI.setChangeListener(new ChangeListener() {
            @Override
            public void doChanger() {
                trash = trashUI.getTrash();
            }
        });
        trashUI.open();
    }

}

