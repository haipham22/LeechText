package dark.leech.text.ui.main;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;

import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.listeners.RemoveListener;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMScrollPane;
import dark.leech.text.ui.repository.AddRepositoriesDialog;
import dark.leech.text.ui.repository.RepositoryTile;

public class RepositoryUI extends JMDialog implements RemoveListener {
    private int numRepository = 0;

    private GridBagConstraints gbc;

    private Set<RepositoryEntity> repositoryList;
    private BasicButton add;
    private BasicButton ok;
    private BasicButton cancel;
    private JPanel body;
    private boolean done;

    public RepositoryUI() {
        numRepository = 0;
        this.repositoryList = new HashSet<>();
        add = new BasicButton();
        ok = new BasicButton();
        cancel = new BasicButton();
        body = new JPanel(new GridBagLayout());
        body.setBackground(Color.white);
        onCreate();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        add = new BasicButton();
        ok = new BasicButton();
        cancel = new BasicButton();
        body = new JPanel(new GridBagLayout());
        body.setBackground(Color.white);
        PanelTitle pnTitle = new PanelTitle();

        pnTitle.setText("Repository");
        pnTitle.addCloseListener(e -> close());
        pnTitle.setBounds(0, 0, 330, 45);

        container.add(pnTitle);

        body.setBackground(Color.white);
        GridBagConstraints gi = new GridBagConstraints();
        gi.gridwidth = GridBagConstraints.REMAINDER;
        gi.weightx = 1;
        gi.weighty = 1;
        JMScrollPane scrollPane = new JMScrollPane(body);

        JPanel demo = new JPanel();
        demo.setBackground(Color.WHITE);
        body.add(demo, gi);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        container.add(scrollPane);
        scrollPane.setBounds(0, 45, 327, 270);
        //
        add.setText("THÊM");
        add.addActionListener(e -> addItem());
        container.add(add);
        add.setBounds(10, 320, 100, 30);

        ok.setText("OK");
        ok.addActionListener(
                e -> {
                    if (done) {
                        close();
                    }
                });
        container.add(ok);
        ok.setBounds(170, 320, 70, 30);

        cancel.setText("HỦY");
        cancel.addActionListener(e -> close());
        container.add(cancel);
        cancel.setBounds(250, 320, 70, 30);

        gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        setSize(330, 370);
    }

    private void load() {
        for (RepositoryEntity pl : repositoryList) {
            addItem(pl);
        }
    }

    private void addItem() {
        final AddRepositoriesDialog addRepositoriesDialog = new AddRepositoriesDialog();
        addRepositoriesDialog.setBlurListener(this);
        addRepositoriesDialog.setChangeListener(() -> {});

        addRepositoriesDialog.open();
    }

    private void addItem(RepositoryEntity repositoryEntity) {
        RepositoryTile repositoryTile = new RepositoryTile(repositoryEntity);
        repositoryTile.setRemoveListener(this);
        body.add(repositoryTile, gbc, numRepository);
        body.updateUI();
        numRepository++;
    }

    private void removeItem(RepositoryTile repositoryTile) {
        repositoryList.remove(repositoryTile.getRepositoryEntity());
        body.remove(repositoryTile);
        numRepository--;
    }

    @Override
    public void removeComponent(Component comp) {
        if (comp instanceof RepositoryTile) {
            removeItem((RepositoryTile) comp);
            body.revalidate();
            body.repaint();
        }
    }
}
