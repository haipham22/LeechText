package dark.leech.text.ui.main.repository;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dark.leech.text.action.Log;
import dark.leech.text.enities.RepositoryEntity;
import dark.leech.text.lua.api.Json;
import dark.leech.text.repository.RepositoryManager;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMScrollPane;
import dark.leech.text.ui.notification.Alert;
import dark.leech.text.ui.notification.Toast;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RepositoryUI extends JMDialog {
    private PanelTitle pnTitle;
    private JPanel pnList;
    private GridBagConstraints gbc;
    private CircleButton btAdd;


    public RepositoryUI() {
        onCreate();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        pnTitle = new PanelTitle();
        pnList = new JPanel(new GridBagLayout());

        pnTitle.setText("Repository");

        btAdd = new CircleButton(StringUtils.ADD, 25f);
        btAdd.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //TODO: add repository btn
            }
        });
        pnTitle.add(btAdd);
        btAdd.setBounds(305, 0, 45, 45);


        pnTitle.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Set<RepositoryEntity> repositoryList = RepositoryManager.getManager().getRepositoryList();
                            JSONArray json = new JSONArray();
                            Gson gson = new Gson();

                            for (RepositoryEntity repository : repositoryList) {
                                //TODO: get repository item and save json
                                json.put(gson.toJson(repository, RepositoryEntity.class));
                            }

                            FileUtils.string2file(
                                    json.toString(),
                                    AppUtils.curDir  + "/tools/repository.json"
                            );
                        } catch (Exception err) {
                            Log.add(err);
                            Alert.show("Có lỗi xảy ra, vui lòng báo lỗi tại github")
                                    .open();
                        }
                    }
                }).start();

                close();
            }
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
        scrollPane.setBounds(0, 45, 380, 350);


        gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (RepositoryEntity repositoryEntity : RepositoryManager.getManager().getRepositoryList())
                    addRepository(repositoryEntity);
            }
        });

        setSize(380, 400);

    }

    private void addRepository(RepositoryEntity repositoryEntity) {
        RepositoryItem repositoryItem = new RepositoryItem(repositoryEntity);
        pnList.add(repositoryItem, gbc, 0);
        validate();
        repaint();
    }
}
