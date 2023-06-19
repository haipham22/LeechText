package dark.leech.text.ui.main.plugin;

import com.google.gson.Gson;
import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.PluginManager;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMScrollPane;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Long on 1/11/2017.
 */
public class PluginUI extends JMDialog {
    private PanelTitle pnTitle;
    private JPanel pnList;
    private GridBagConstraints gbc;

    public PluginUI() {
        onCreate();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        pnTitle = new PanelTitle();
        pnList = new JPanel(new GridBagLayout());

        pnTitle.setText("Plugins");
        pnTitle.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (PluginEntity pl : PluginManager.getManager().list()) {
                            String path = AppUtils.curDir
                                    + "/tools/plugins/"
                                    + pl.getPathName()
                                    + ".plugin";
                            FileUtils.string2file(new Gson().toJson(pl), path);

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
                for (PluginEntity pluginGetter : PluginManager.getManager().list())
                    addItem(pluginGetter);
            }
        });

        setSize(380, 400);

    }

    private void addItem(PluginEntity pluginGetter) {
        PluginItem pluginItem = new PluginItem(pluginGetter);
        pnList.add(pluginItem, gbc, 0);
        validate();
        repaint();
    }
}
