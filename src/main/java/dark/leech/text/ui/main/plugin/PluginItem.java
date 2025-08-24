package dark.leech.text.ui.main.plugin;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.image.ImageLabel;
import dark.leech.text.ui.button.SelectButton;
import dark.leech.text.ui.material.DropShadowBorder;
import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.util.Base64;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.SettingUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Created by Long on 1/11/2017.
 */
public class PluginItem extends JMPanel {
    private ImageLabel pnIcon;
    private JLabel lbName;
    private JLabel lbInfo;
    private SelectButton btCheckBox;
    private PluginEntity pluginGetter;

    public PluginItem(PluginEntity pluginGetter) {
        this.pluginGetter = pluginGetter;
        onCreate();
    }

    private void onCreate() {
        pnIcon = new ImageLabel();
        lbName = new JLabel();
        lbInfo = new JLabel();
        btCheckBox = new SelectButton();
        //---- pnIcon ----
        add(pnIcon);
        pnIcon.setBounds(10, 5, 55, 55);
        if (pluginGetter.getIcon() == null) {
            pnIcon.input(PluginItem.class.getResourceAsStream("/dark/leech/res/img/book.png"))
                    .load();
        } else {
            InputStream in = new ByteArrayInputStream(Base64.decode(pluginGetter.getIcon()));
            pnIcon.input(in).load();
        }

        //---- lbName ----
        lbName.setText(pluginGetter.getName() + " - v" + pluginGetter.getVersion());
        lbName.setFont(FontUtils.TEXT_NORMAL);
        add(lbName);
        lbName.setBounds(70, 5, 220, 25);

        //---- lbInfo ----
        lbInfo.setText(pluginGetter.getSource());
        lbInfo.setFont(FontUtils.TEXT_THIN);
        add(lbInfo);
        lbInfo.setBounds(70, 35, 295, 25);

        //---- btCheckBox ----
        btCheckBox.setSelected(pluginGetter.isChecked());
        btCheckBox.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                pluginGetter.setChecked(btCheckBox.isSelected());
            }
        });
        add(btCheckBox);
        btCheckBox.setBounds(340, 5, 30, 30);

        setBorder(new DropShadowBorder(SettingUtils.THEME_COLOR, 5, 3));
        setPreferredSize(new Dimension(375, 70));

    }


}
