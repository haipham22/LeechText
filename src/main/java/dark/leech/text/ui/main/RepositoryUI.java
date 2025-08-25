package dark.leech.text.ui.main;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;

import javax.swing.*;

import com.google.gson.Gson;

import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.listeners.RemoveListener;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMScrollPane;
import dark.leech.text.ui.notification.Toast;
import dark.leech.text.ui.repository.AddRepositoriesDialog;
import dark.leech.text.ui.repository.RepositoryTile;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.FontUtils;

public class RepositoryUI extends JMDialog implements RemoveListener {
    private PanelTitle pnTitle;
    private JPanel pnList;
    private GridBagConstraints gbc;
    private Set<RepositoryEntity> repositoryList;

    private int numRepository = 0;

    private static final Gson gson = new Gson();

    public RepositoryUI() {
        repositoryList = new HashSet<>();
        onCreate();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        registerButton();
        pnTitle = new PanelTitle();
        pnList = new JPanel(new GridBagLayout());

        pnTitle.setText("Repository");
        pnTitle.addCloseListener(
                e -> {
                    new Thread(
                                    () -> {
                                        //                                        for (PluginEntity
                                        // pl : PluginManager.getManager().list()) {
                                        //                                            String path =
                                        //
                                        // AppUtils.curDir
                                        //
                                        //  + "/tools/plugins/"
                                        //
                                        //  + pl.getUuid()
                                        //
                                        //  + ".plugin";
                                        //
                                        // FileUtils.string2file(new Gson().toJson(pl), path);
                                        //                                        }
                                    })
                            .start();

                    close();
                });
        container.add(pnTitle);
        pnTitle.setBounds(0, 0, 380, 45);

        pnList.setBackground(Color.WHITE);
        GridBagConstraints gi = new GridBagConstraints();
        gi.gridwidth = GridBagConstraints.REMAINDER;
        gi.weightx = 1;
        gi.weighty = 1;
        JMScrollPane scrollPane = new JMScrollPane(pnList);

        JPanel demo = new JPanel();
        demo.setBackground(Color.WHITE);
        pnList.add(demo, gi);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        container.add(scrollPane);
        scrollPane.setBounds(0, 45, 380, 300);

        gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        runOnUiThread(
                () -> {
                    refreshList();
                });

        setSize(380, 400);
    }

    private void registerButton() {
        var ok = new BasicButton();
        ok.setText("OK");
        ok.addActionListener(e -> save());
        ok.setBounds(160, 350, 100, 30);

        var cancel = new BasicButton();
        cancel.setText("HỦY");
        cancel.addActionListener(e -> close());
        cancel.setBounds(260, 350, 100, 30);

        var add = new BasicButton();
        add.setText("THÊM");
        add.addActionListener(e -> onAddItem());
        add.setBounds(10, 350, 100, 30);

        container.add(ok);
        container.add(cancel);
        container.add(add);
    }

    private void onAddItem() {
        final var dialog = new AddRepositoriesDialog();
        dialog.setBlurListener(this);
        dialog.setChangeListener(
                () -> {
                    if (dialog.getRepositoryList() == null
                            || dialog.getRepositoryList().isEmpty()) {
                        return;
                    }

                    for (RepositoryEntity repositoryEntity : dialog.getRepositoryList()) {
                        repositoryEntity.setSelected(true);
                        repositoryList.add(repositoryEntity);
                    }

                    refreshList();
                });
        dialog.open();
    }

    private void addItem(RepositoryEntity repositoryEntity) {
        RepositoryTile repositoryItem = new RepositoryTile(repositoryEntity);
        repositoryItem.setRemoveListener(this);
        repositoryItem.setBlurListener(this);
        pnList.add(repositoryItem, gbc, numRepository);
        validate();
        repaint();
        numRepository++;
    }

    private void addFillerRow() {
        GridBagConstraints fillerConstraints = new GridBagConstraints();
        fillerConstraints.gridwidth = GridBagConstraints.REMAINDER;
        fillerConstraints.weightx = 1;
        fillerConstraints.weighty = 1;
        JPanel filler = new JPanel();
        filler.setBackground(Color.WHITE);
        pnList.add(filler, fillerConstraints);
    }

    private void refreshList() {
        pnList.removeAll();
        numRepository = 0;
        for (RepositoryEntity repositoryEntity : repositoryList) {
            addItem(repositoryEntity);
        }
        addFillerRow();
        pnList.revalidate();
        pnList.repaint();
    }

    @Override
    public void removeComponent(Component comp) {
        if (comp instanceof RepositoryTile) {
            repositoryList.remove(((RepositoryTile) comp).getRepositoryEntity());
            pnList.remove(comp);
            pnList.updateUI();
            numRepository--;
        }
    }

    public void save() {
        String json = gson.toJson(repositoryList);
        FileUtils.string2file(json, AppUtils.curDir + "/tools/repository.json");
        Toast.Build().font(FontUtils.TITLE_NORMAL).content("Đã lưu repository!").open();
        close();
    }
}
