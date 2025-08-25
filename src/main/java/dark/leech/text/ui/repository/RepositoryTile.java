package dark.leech.text.ui.repository;

import java.awt.*;

import javax.swing.*;

import lombok.Getter;
import lombok.Setter;

import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.listeners.RemoveListener;
import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.StringUtils;

public class RepositoryTile extends JMPanel {
    private JLabel labelName;
    private CircleButton buttonDelete;

    @Getter private RepositoryEntity repositoryEntity;

    @Setter private RemoveListener removeListener;

    public RepositoryTile(RepositoryEntity repositoryEntity) {
        this.repositoryEntity = repositoryEntity;
        gui();
    }

    private void gui() {
        setBackground(Color.white);
        setLayout(null);

        labelName = new JLabel();

        labelName.setText(repositoryEntity.getDescription());
        labelName.setFont(FontUtils.TEXT_NORMAL);
        add(labelName);
        labelName.setBounds(10, 5, 180, 30);

        buttonDelete = new CircleButton(StringUtils.DELETE);
        buttonDelete.setForeground(ColorUtils.THEME_COLOR);
        buttonDelete.setToolTipText("Xóa");
        buttonDelete.addActionListener(e -> actionDelete());
        add(buttonDelete);
        buttonDelete.setBounds(280, 5, 30, 30);
    }

    private void actionDelete() {
        removeListener.removeComponent(this);
    }
}
