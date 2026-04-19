package dark.leech.text.ui.main.plugin;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import dark.leech.text.plugin.vbook.VBookPluginService;
import dark.leech.text.plugin.vbook.exception.VBookRegistryException;
import dark.leech.text.plugin.vbook.model.VBookExtensionEntity;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.button.SelectButton;
import dark.leech.text.ui.material.*;
import dark.leech.text.ui.notification.Toast;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.SettingUtils;
import dark.leech.text.util.StringUtils;

/** UI for browsing and installing vBook extensions. Matches LeechText app UI style. */
public class VBookPluginBrowserUI extends JMDialog {

    private PanelTitle pnTitle;
    private JPanel pnList;
    private GridBagConstraints gbc;
    private List<VBookExtensionEntity> extensionList;
    private List<VBookExtensionEntity> allExtensions;
    private JMTextField textSearch;
    private String selectedType = "all";
    private SelectButton btAll, btNovel, btComic;

    public VBookPluginBrowserUI() {
        onCreate();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        registerButton();
        pnTitle = new PanelTitle();
        pnList = new JPanel(new GridBagLayout());
        textSearch = new JMTextField();

        pnTitle.setText("vBook Extensions");
        pnTitle.addCloseListener(e -> close());

        container.add(pnTitle);
        pnTitle.setBounds(0, 0, 380, 45);

        // Search field
        textSearch.setFont(FontUtils.TEXT_THIN);
        container.add(textSearch);
        textSearch.setBounds(10, 50, 360, 30);

        // Type filter buttons
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(null);
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBounds(10, 85, 360, 25);
        container.add(filterPanel);

        // All filter
        JLabel lbAll = new JLabel("Tất cả");
        lbAll.setFont(FontUtils.TEXT_NORMAL);
        filterPanel.add(lbAll);
        lbAll.setBounds(0, 0, 60, 25);

        btAll = new SelectButton();
        btAll.setSelected(true);
        btAll.setFont(FontUtils.ICON_NORMAL);
        btAll.addChangeListener(
                e -> {
                    if (btAll.isSelected()) {
                        selectedType = "all";
                        btNovel.setSelected(false);
                        btComic.setSelected(false);
                        btAll.setSelected(true);
                        filterAndShowExtensions();
                    }
                });
        filterPanel.add(btAll);
        btAll.setBounds(65, 0, 25, 25);

        // Novel filter
        JLabel lbNovel = new JLabel("Novel");
        lbNovel.setFont(FontUtils.TEXT_NORMAL);
        filterPanel.add(lbNovel);
        lbNovel.setBounds(100, 0, 60, 25);

        btNovel = new SelectButton();
        btNovel.setSelected(false);
        btNovel.setFont(FontUtils.ICON_NORMAL);
        btNovel.addChangeListener(
                e -> {
                    if (btNovel.isSelected()) {
                        selectedType = "novel";
                        btAll.setSelected(false);
                        btComic.setSelected(false);
                        btNovel.setSelected(true);
                        filterAndShowExtensions();
                    }
                });
        filterPanel.add(btNovel);
        btNovel.setBounds(165, 0, 25, 25);

        // Comic filter
        JLabel lbComic = new JLabel("Comic");
        lbComic.setFont(FontUtils.TEXT_NORMAL);
        filterPanel.add(lbComic);
        lbComic.setBounds(200, 0, 60, 25);

        btComic = new SelectButton();
        btComic.setSelected(false);
        btComic.setFont(FontUtils.ICON_NORMAL);
        btComic.addChangeListener(
                e -> {
                    if (btComic.isSelected()) {
                        selectedType = "comic";
                        btAll.setSelected(false);
                        btNovel.setSelected(false);
                        btComic.setSelected(true);
                        filterAndShowExtensions();
                    }
                });
        filterPanel.add(btComic);
        btComic.setBounds(265, 0, 25, 25);

        textSearch
                .getDocument()
                .addDocumentListener(
                        new DocumentListener() {
                            @Override
                            public void insertUpdate(DocumentEvent e) {
                                filterAndShowExtensions();
                            }

                            @Override
                            public void removeUpdate(DocumentEvent e) {
                                filterAndShowExtensions();
                            }

                            @Override
                            public void changedUpdate(DocumentEvent e) {
                                filterAndShowExtensions();
                            }
                        });

        pnList.setBackground(Color.WHITE);
        GridBagConstraints gi = new GridBagConstraints();
        gi.gridwidth = GridBagConstraints.REMAINDER;
        gi.weightx = 1;
        gi.weighty = 1;
        JMScrollPane scrollPane = new JMScrollPane(pnList);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);

        container.add(scrollPane);
        scrollPane.setBounds(0, 115, 380, 235);

        gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Show loading
        JLabel loading = new JLabel("Đang tải extensions...", JLabel.CENTER);
        loading.setFont(FontUtils.TEXT_NORMAL);
        loading.setForeground(Color.GRAY);
        pnList.add(loading, gbc, 0);

        // Load plugins in background
        new Thread(this::refreshPlugins, "vBook-Load").start();

        setSize(380, 400);
    }

    private void registerButton() {
        var refresh = new CircleButton(StringUtils.SEARCH, 20f);
        refresh.setBounds(15, 360, 30, 30);
        refresh.addActionListener(e -> refreshPlugins());
        container.add(refresh);
    }

    private void refreshPlugins() {
        SwingUtilities.invokeLater(
                () -> {
                    pnList.removeAll();
                    gbc = new GridBagConstraints();
                    gbc.gridwidth = GridBagConstraints.REMAINDER;
                    gbc.weightx = 1;
                    gbc.fill = GridBagConstraints.HORIZONTAL;

                    // Show loading
                    JLabel loading = new JLabel("Đang tải extensions...", JLabel.CENTER);
                    loading.setFont(FontUtils.TEXT_NORMAL);
                    loading.setForeground(Color.GRAY);
                    pnList.add(loading, gbc, 0);
                    pnList.revalidate();
                    pnList.repaint();
                });

        new Thread(
                        () -> {
                            try {
                                VBookPluginService service = new VBookPluginService();
                                extensionList = service.getAvailablePlugins();
                                allExtensions = new ArrayList<>(extensionList);

                                SwingUtilities.invokeLater(this::filterAndShowExtensions);

                            } catch (VBookRegistryException e) {
                                SwingUtilities.invokeLater(
                                        () -> {
                                            pnList.removeAll();
                                            gbc = new GridBagConstraints();
                                            gbc.gridwidth = GridBagConstraints.REMAINDER;
                                            gbc.weightx = 1;
                                            gbc.fill = GridBagConstraints.HORIZONTAL;

                                            JLabel error =
                                                    new JLabel(
                                                            "Lỗi: " + e.getMessage(),
                                                            JLabel.CENTER);
                                            error.setFont(FontUtils.TEXT_NORMAL);
                                            error.setForeground(Color.RED);
                                            pnList.add(error, gbc, 0);
                                            pnList.revalidate();
                                            pnList.repaint();
                                        });
                            }
                        },
                        "vBook-Refresh")
                .start();
    }

    private void filterAndShowExtensions() {
        SwingUtilities.invokeLater(
                () -> {
                    pnList.removeAll();
                    gbc = new GridBagConstraints();
                    gbc.gridwidth = GridBagConstraints.REMAINDER;
                    gbc.weightx = 1;
                    gbc.fill = GridBagConstraints.HORIZONTAL;

                    if (allExtensions == null) {
                        JLabel loading = new JLabel("Đang tải extensions...", JLabel.CENTER);
                        loading.setFont(FontUtils.TEXT_NORMAL);
                        loading.setForeground(Color.GRAY);
                        pnList.add(loading, gbc, 0);
                        addFillerRow();
                        pnList.revalidate();
                        pnList.repaint();
                        return;
                    }

                    String searchText = textSearch.getText().trim().toLowerCase();
                    List<VBookExtensionEntity> filteredExtensions = allExtensions;

                    // Apply type filter first
                    if (!"all".equals(selectedType)) {
                        final String typeFilter = selectedType;
                        filteredExtensions =
                                filteredExtensions.stream()
                                        .filter(e -> typeFilter.equals(e.getType()))
                                        .toList();
                    }

                    // Then apply text search filter
                    if (!searchText.isEmpty()) {
                        final String finalSearchText = searchText;
                        filteredExtensions =
                                filteredExtensions.stream()
                                        .filter(
                                                e ->
                                                        e.getName()
                                                                        .toLowerCase()
                                                                        .contains(finalSearchText)
                                                                || (e.getAuthor() != null
                                                                        && e.getAuthor()
                                                                                .toLowerCase()
                                                                                .contains(
                                                                                        finalSearchText))
                                                                || (e.getSource() != null
                                                                        && e.getSource()
                                                                                .toLowerCase()
                                                                                .contains(
                                                                                        finalSearchText))
                                                                || (e.getPath() != null
                                                                        && e.getPath()
                                                                                .toLowerCase()
                                                                                .contains(
                                                                                        finalSearchText)))
                                        .toList();
                    }

                    if (filteredExtensions.isEmpty()) {
                        JLabel empty = new JLabel("Không có extension nào", JLabel.CENTER);
                        empty.setFont(FontUtils.TEXT_NORMAL);
                        empty.setForeground(Color.GRAY);
                        pnList.add(empty, gbc, 0);
                    } else {
                        int index = 0;
                        for (VBookExtensionEntity ext : filteredExtensions) {
                            VBookExtensionItem item = new VBookExtensionItem(ext);
                            pnList.add(item, gbc, index++);
                        }
                    }

                    addFillerRow();
                    pnList.revalidate();
                    pnList.repaint();
                });
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

    /** UI component for a single vBook extension. Matches LeechText UI style. */
    private static class VBookExtensionItem extends JMPanel {

        private final VBookExtensionEntity extension;
        private SelectButton btInstall;

        public VBookExtensionItem(VBookExtensionEntity extension) {
            this.extension = extension;
            onCreate();
        }

        private void onCreate() {
            setBackground(Color.WHITE);
            setLayout(null);
            setBorder(new DropShadowBorder(SettingUtils.THEME_COLOR, 5, 3));
            setPreferredSize(new Dimension(340, 75));

            // Icon
            JLabel iconLabel = new JLabel("📦");
            iconLabel.setFont(new Font("Dialog", Font.PLAIN, 24));
            add(iconLabel);
            iconLabel.setBounds(10, 10, 40, 40);

            // Name
            JLabel nameLabel = new JLabel(extension.getName());
            nameLabel.setFont(FontUtils.TEXT_NORMAL);
            add(nameLabel);
            nameLabel.setBounds(55, 10, 200, 20);

            // Author
            JLabel authorLabel = new JLabel("by " + extension.getAuthor());
            authorLabel.setFont(FontUtils.TEXT_THIN);
            add(authorLabel);
            authorLabel.setBounds(55, 32, 200, 15);

            // Description (truncated)
            String desc = extension.getDescription();
            if (desc != null && desc.length() > 40) {
                desc = desc.substring(0, 37) + "...";
            }
            JLabel descLabel = new JLabel(desc);
            descLabel.setFont(FontUtils.TEXT_THIN);
            add(descLabel);
            descLabel.setBounds(55, 50, 200, 15);

            // Install button
            btInstall = new SelectButton();
            btInstall.setText("Cài");
            btInstall.setSelected(false);
            btInstall.addChangeListener(
                    e -> {
                        if (btInstall.isSelected()) {
                            installExtension();
                            btInstall.setSelected(false);
                        }
                    });
            add(btInstall);
            btInstall.setBounds(280, 22, 50, 30);

            // Delete/remove button (optional - closes the item)
            CircleButton btClose = getCircleButton();
            add(btClose);
            btClose.setBounds(300, 25, 25, 25);
        }

        private CircleButton getCircleButton() {
            CircleButton btClose = new CircleButton(StringUtils.CLOSE, 15f);
            btClose.setForeground(ColorUtils.THEME_COLOR);
            btClose.addActionListener(
                    e -> {
                        // Could show more info or remove from list
                        JOptionPane.showMessageDialog(
                                VBookExtensionItem.this,
                                "Plugin: "
                                        + extension.getName()
                                        + "\nAuthor: "
                                        + extension.getAuthor()
                                        + "\nVersion: "
                                        + extension.getVersion()
                                        + "\nType: "
                                        + extension.getType(),
                                "Plugin Info",
                                JOptionPane.INFORMATION_MESSAGE);
                    });
            return btClose;
        }

        private void installExtension() {
            new Thread(
                            () -> {
                                try {
                                    VBookPluginService service = new VBookPluginService();
                                    boolean success = service.installPlugin(extension);

                                    SwingUtilities.invokeLater(
                                            () -> {
                                                if (success) {
                                                    Toast.Build()
                                                            .content(
                                                                    "Đã cài đặt: "
                                                                            + extension.getName())
                                                            .time(3000)
                                                            .open();
                                                } else {
                                                    Toast.Build()
                                                            .content(
                                                                    "Cài đặt thất bại: "
                                                                            + extension.getName())
                                                            .time(3000)
                                                            .open();
                                                }
                                            });
                                } catch (Exception ex) {
                                    SwingUtilities.invokeLater(
                                            () -> {
                                                Toast.Build()
                                                        .content("Lỗi: " + ex.getMessage())
                                                        .time(3000)
                                                        .open();
                                            });
                                }
                            },
                            "vBook-Install")
                    .start();
        }
    }
}
