package dark.leech.text.ui.main.plugin;

import static dark.leech.text.plugin.PluginManager.removeListener;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.PluginManager;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMScrollPane;
import dark.leech.text.ui.material.JMTextField;
import dark.leech.text.util.FontUtils;

public class PluginUI extends JMDialog {
    private PanelTitle pnTitle;
    private JPanel pnList;
    private GridBagConstraints gbc;
    private JMTextField textSearch;
    private final PluginManager.PluginListListener pluginListener;
    private List<PluginEntity> allPlugins;

    public PluginUI() {
        // Register listener for plugin changes
        pluginListener = this::refreshPluginList;
        PluginManager.addListener(pluginListener);
        onCreate();
    }

    @Override
    public void close() {
        removeListener(pluginListener);
        super.close();
    }

    private void refreshPluginList() {
        allPlugins = new ArrayList<>(PluginManager.getManager().list());
        filterAndShowPlugins();
    }

    private void filterAndShowPlugins() {
        SwingUtilities.invokeLater(
                () -> {
                    pnList.removeAll();
                    int index = 0;

                    String searchText = textSearch.getText().trim().toLowerCase();
                    List<PluginEntity> filteredPlugins = allPlugins;

                    if (!searchText.isEmpty()) {
                        filteredPlugins =
                                allPlugins.stream()
                                        .filter(p -> p.getName().toLowerCase().contains(searchText))
                                        .collect(Collectors.toList());
                    }

                    for (PluginEntity plugin : filteredPlugins) {
                        addItem(plugin, index++);
                    }
                    addFillerRow();
                    pnList.revalidate();
                    pnList.repaint();
                });
    }

    private void addItem(PluginEntity pluginGetter, int index) {
        PluginItem pluginItem = new PluginItem(pluginGetter);
        pnList.add(pluginItem, gbc, index);
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

    @Override
    protected void onCreate() {
        super.onCreate();
        pnTitle = new PanelTitle();
        pnList = new JPanel(new GridBagLayout());
        textSearch = new JMTextField();

        pnTitle.setText("Plugins");
        pnTitle.addCloseListener(
                e -> {
                    getPlugin();

                    close();
                });
        container.add(pnTitle);
        pnTitle.setBounds(0, 0, 380, 45);

        // Search field
        textSearch.setFont(FontUtils.TEXT_THIN);
        container.add(textSearch);
        textSearch.setBounds(10, 50, 360, 30);

        textSearch
                .getDocument()
                .addDocumentListener(
                        new DocumentListener() {
                            @Override
                            public void insertUpdate(DocumentEvent e) {
                                filterAndShowPlugins();
                            }

                            @Override
                            public void removeUpdate(DocumentEvent e) {
                                filterAndShowPlugins();
                            }

                            @Override
                            public void changedUpdate(DocumentEvent e) {
                                filterAndShowPlugins();
                            }
                        });

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
        scrollPane.setBounds(0, 90, 380, 280);

        gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Initial load - wait for plugins to be loaded/downloaded
        runOnUiThread(
                () -> {
                    PluginManager.waitForInitialization();
                    refreshPluginList();
                });

        setSize(380, 420);
    }

    private void getPlugin() {
        // Manual plugin management is available in VBookPluginBrowserUI
        // Auto-download removed as requested - use manual selection instead
    }
}
