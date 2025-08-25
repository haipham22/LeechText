package dark.leech.text.ui.repository;

import java.awt.*;

import javax.swing.*;

import lombok.Getter;
import lombok.Setter;

import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.listeners.BlurListener;
import dark.leech.text.listeners.RemoveListener;
import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.button.SelectButton;
import dark.leech.text.ui.material.DropShadowBorder;
import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.SettingUtils;
import dark.leech.text.util.StringUtils;

public class RepositoryTile extends JMPanel {

    @Getter private RepositoryEntity repositoryEntity;

    @Setter private RemoveListener removeListener;
    @Setter private BlurListener blurListener;

    public RepositoryTile(RepositoryEntity repositoryEntity) {
        this.repositoryEntity = repositoryEntity;
        onCreate();
    }

    private void onCreate() {
        setBackground(Color.white);
        setLayout(null);

        var labelName = new JLabel();
        labelName.setText(repositoryEntity.getAuthor());
        labelName.setFont(FontUtils.TEXT_NORMAL);
        add(labelName);
        labelName.setBounds(10, 5, 280, 30);

        var labelDescription = new JLabel();
        labelDescription.setText(repositoryEntity.getDescription());
        labelDescription.setFont(FontUtils.TEXT_THIN);
        add(labelDescription);
        labelDescription.setBounds(10, 30, 280, 30);

        var labelLink = new JLabel();
        labelLink.setText(repositoryEntity.getLink());
        labelLink.setFont(FontUtils.TEXT_THIN);
        add(labelLink);
        labelLink.setBounds(10, 60, 280, 30);

        var btSelect = new SelectButton();
        btSelect.setSelected(repositoryEntity.isEnabled());
        btSelect.addActionListener(
                e -> {
                    repositoryEntity.setEnabled(!repositoryEntity.isEnabled());
                    blurListener.setBlur(true);
                });
        add(btSelect);
        btSelect.setBounds(280, 5, 30, 30);

        var buttonDelete = new CircleButton(StringUtils.DELETE);
        buttonDelete.setForeground(ColorUtils.THEME_COLOR);
        buttonDelete.setToolTipText("Xóa");
        buttonDelete.addActionListener(e -> actionDelete());

        add(buttonDelete);

        buttonDelete.setBounds(310, 5, 30, 30);

        setBorder(new DropShadowBorder(SettingUtils.THEME_COLOR, 5, 3));
        setPreferredSize(new Dimension(340, 90));
    }

    private void actionDelete() {
        blurListener.setBlur(true);
        removeListener.removeComponent(this);
    }
}
