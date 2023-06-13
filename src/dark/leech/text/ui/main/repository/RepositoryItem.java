package dark.leech.text.ui.main.repository;

import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.ui.button.SelectButton;
import dark.leech.text.ui.material.DropShadowBorder;
import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.SettingUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Created by Long on 1/11/2017.
 */
public class RepositoryItem extends JMPanel {
    private JLabel lbAuthor;
    private JLabel lbInfo;
    private SelectButton btCheckBox;
    private RepositoryEntity repository;

    public RepositoryItem(RepositoryEntity repository) {
        this.repository = repository;
        onCreate();
    }

    private void onCreate() {
        lbAuthor = new JLabel();
        lbInfo = new JLabel();
        btCheckBox = new SelectButton();

        //---- lbAuthor ----
        lbAuthor.setText(repository.getAuthor());
        lbAuthor.setFont(FontUtils.TEXT_NORMAL);
        add(lbAuthor);
        lbAuthor.setBounds(5, 5, 220, 25);

        //---- lbInfo ----
        lbInfo.setText(repository.getDescription());
        lbInfo.setFont(FontUtils.TEXT_THIN);
        add(lbInfo);
        lbInfo.setBounds(5, 35, 295, 25);

        //---- btCheckBox ----
        btCheckBox.setSelected(repository.isChecked());
        btCheckBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                repository.setChecked(btCheckBox.isSelected());
            }
        });
        add(btCheckBox);
        btCheckBox.setBounds(340, 5, 30, 30);

        setBorder(new DropShadowBorder(SettingUtils.THEME_COLOR, 5, 3));
        setPreferredSize(new Dimension(375, 70));

    }


}
