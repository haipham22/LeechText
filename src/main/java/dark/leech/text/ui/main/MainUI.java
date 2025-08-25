package dark.leech.text.ui.main;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.*;
import javax.swing.border.LineBorder;

import dark.leech.text.image.GaussianBlurFilter;
import dark.leech.text.listeners.BlurListener;
import dark.leech.text.plugin.PluginManager;
import dark.leech.text.plugin.PluginUpdate;
import dark.leech.text.plugin.RepositoryManager;
import dark.leech.text.ui.Animation;
import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.button.CloseButton;
import dark.leech.text.ui.download.AddURL;
import dark.leech.text.ui.download.DownloadUI;
import dark.leech.text.ui.main.plugin.PluginUI;
import dark.leech.text.ui.material.JMMenuItem;
import dark.leech.text.ui.material.JMPopupMenu;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.GraphicsUtils;
import dark.leech.text.util.StringUtils;

/**
 * Main user interface for the LeechText application.
 *
 * <p>MainUI is the primary window of the application that provides access to all major features
 * including download management, settings, plugins, and help. The interface is designed with a
 * modern, clean aesthetic featuring custom buttons, animations, and a responsive layout.
 *
 * <p>The UI consists of several key components:
 *
 * <ul>
 *   <li>Status bar - displays current time and provides window dragging functionality
 *   <li>App bar - contains the main logo and action buttons (add, menu)
 *   <li>Download UI - main content area for managing downloads
 *   <li>Settings UI - configuration and preferences panel
 *   <li>Plugin management - interface for managing text extraction plugins
 * </ul>
 *
 * <p>The interface supports window dragging, fade animations, and dynamic content switching between
 * different functional areas.
 *
 * @author LeechText Development Team
 * @version 1.0
 * @since 1.0
 * @see DownloadUI
 * @see SettingUI
 * @see PluginUI
 */
public class MainUI extends JFrame implements BlurListener, ActionListener {
    private DownloadUI downloadUI;
    private SettingUI setting;
    private RepositoryUI repositoryUI;

    private JPanel appBar;

    private CircleButton btAdd;

    private CircleButton btMenu;

    private JPanel statusBar;

    private CircleButton btBack;

    private CircleButton btOk;

    private JLabel lbLogo;

    private JPanel pnHeader;

    private JMPopupMenu menu;
    private JMMenuItem pnSetting;
    private JMenuItem pnHelp;
    private JMMenuItem pnPlugin;
    private JMMenuItem pnRepository;

    private CloseButton btExit;

    private JLabel lbStatus;

    private Container container;

    private Point initialClick;

    private BufferedImage blurBuffer;

    private BufferedImage backBuffer;

    private Timer timer;

    private int i = 0;

    public MainUI() {
        // setLocation(AppUtils.width - 420, AppUtils.height - 650);
        setSize(390, 555);
        getRootPane().setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
        setUndecorated(true);
        setTitle("LeechText");
        setIconImage(
                Toolkit.getDefaultToolkit()
                        .getImage(getClass().getResource("/dark/leech/res/icon.png")));
        new Thread(this::onCreate).start();
    }

    /**
     * Initializes all UI components and layouts.
     *
     * <p>This method is called in a separate thread to ensure smooth startup and prevent blocking
     * the main thread during component initialization.
     */
    private void onCreate() {
        container = getContentPane();
        container.setLayout(null);
        container.setBackground(Color.WHITE);
        onCreateStatusBar();
        onCreateAppBar();
        createPanelHeaderUI();
        createPopupMenu();
        ensureRepository();
        downloadUI = new DownloadUI();
        downloadUI.add(appBar);
        appBar.setBounds(0, 0, 390, 55);
        container.add(downloadUI);
        downloadUI.setBounds(0, 20, 390, 535);
        setting = new SettingUI();
        setting.add(pnHeader);
        setting.setBounds(390, 20, 390, 535);
        pnHeader.setBounds(0, 0, 390, 55);
        container.add(setting);
        checkUpdate();
    }

    private void ensureRepository() {
        var manager = RepositoryManager.getManager();
        if (manager.hasRepoSetting()) {
            if (repositoryUI == null) {
                repositoryUI = new RepositoryUI();
            }

            repositoryUI.load();
        }
    }

    /** Handles the exit action with fade-out animation. */
    private void actionExit() {
        Animation.fadeOut(this);
    }

    /** Handles the add action by opening the URL input dialog. */
    private void actionAdd() {
        AddURL addURL = new AddURL();
        addURL.setAddListener(downloadUI);
        addURL.open();
    }

    /**
     * Creates the status bar at the top of the window.
     *
     * <p>The status bar contains the exit button, status label with current time, and provides
     * window dragging functionality. It's positioned at the very top of the window and spans the
     * full width.
     */
    private void onCreateStatusBar() {
        statusBar = new JPanel();
        statusBar.setBackground(ColorUtils.STATUS_BAR);
        statusBar.setLayout(null);

        // Exit button setup
        btExit = new CloseButton();
        btExit.setFont(FontUtils.iconFont(18f));
        btExit.addActionListener(this);
        statusBar.add(btExit);
        btExit.setBounds(360, 0, 20, 20);

        // Status label setup
        lbStatus = new JLabel();
        lbStatus.setForeground(Color.white);
        lbStatus.setFocusable(false);
        lbStatus.setFont(FontUtils.textFont(13f, Font.PLAIN));
        statusBar.add(lbStatus);
        lbStatus.setBounds(5, 0, 315, 20);

        // Timer for updating status display
        timer =
                new Timer(
                        1000,
                        new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                                Date date = new Date();
                                lbStatus.setText(dateFormat.format(date));
                            }
                        });
        container.add(statusBar);
        statusBar.setBounds(0, 0, 390, 20);

        // Mouse listeners for window dragging
        statusBar.addMouseListener(
                new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        initialClick = e.getPoint();
                        getComponentAt(initialClick);
                    }
                });
        statusBar.addMouseMotionListener(
                new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        movieWindows(e);
                    }
                });
    }

    /**
     * Creates the main application bar containing the logo and action buttons.
     *
     * <p>The app bar is positioned below the status bar and contains the application logo, add
     * button for new downloads, and menu button for accessing additional options.
     */
    private void onCreateAppBar() {

        appBar = new JPanel();
        appBar.setBackground(ColorUtils.THEME_COLOR);
        appBar.setLayout(null);

        // Add button for new downloads
        btAdd = new CircleButton(StringUtils.ADD, 25f);
        btAdd.addActionListener(this);
        appBar.add(btAdd);
        btAdd.setBounds(305, 5, 45, 45);

        // Application logo
        JLabel logo;
        logo = new JLabel();
        logo.setText("Leech Text");
        logo.setFont(FontUtils.TITLE_BIG);
        logo.setForeground(Color.white);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        appBar.add(logo);
        logo.setBounds(20, 0, logo.getPreferredSize().width, 55);

        // Menu button
        btMenu = new CircleButton(StringUtils.MORE, 25f);
        btMenu.addActionListener(this);
        appBar.add(btMenu);
        btMenu.setBounds(355, 5, 30, 45);
    }

    /**
     * Creates the header panel for secondary views.
     *
     * <p>This panel contains navigation elements like back button and is used when switching
     * between different functional areas of the application.
     */
    private void createPanelHeaderUI() {
        pnHeader = new JPanel();
        pnHeader.setBackground(ColorUtils.THEME_COLOR);
        pnHeader.setLayout(null);

        btBack = new CircleButton(StringUtils.BACK, 25f);
        btBack.addActionListener(this);
        pnHeader.add(btBack);
        btBack.setBounds(5, 5, 45, 45);

        // Logo for header panel
        lbLogo = new JLabel();
        lbLogo.setText("Leech Text");
        lbLogo.setFont(FontUtils.TITLE_BIG);
        lbLogo.setForeground(Color.white);
        lbLogo.setHorizontalAlignment(SwingConstants.CENTER);
        pnHeader.add(lbLogo);
        lbLogo.setBounds(20, 0, lbLogo.getPreferredSize().width, 55);
    }

    private void createPopupMenu() {
        ActionListener actionListener =
                e -> {
                    if (e.getSource() == pnSetting) {
                        Animation.go(downloadUI, setting);
                        setting.load();
                    }
                    if (e.getSource() == pnHelp) new HelpUI().open();
                    if (e.getSource() == pnPlugin) new PluginUI().open();
                    if (e.getSource() == pnRepository) {
                        final var repoUI = new RepositoryUI();
                        repoUI.setChangeListener(PluginUpdate::getUpdate);
                        repoUI.open();
                    }
                    ;
                };

        menu = new JMPopupMenu();
        pnSetting = new JMMenuItem("Cài đặt");
        pnSetting.addActionListener(actionListener);
        menu.add(pnSetting);

        pnRepository = new JMMenuItem("Repositories");
        pnRepository.addActionListener(actionListener);
        menu.add(pnRepository);

        pnPlugin = new JMMenuItem("Plugins");
        pnPlugin.addActionListener(actionListener);
        menu.add(pnPlugin);

        pnHelp = new JMMenuItem("Thông tin");
        pnHelp.addActionListener(actionListener);
        menu.add(pnHelp);

        container.add(menu);
    }

    private void movieWindows(MouseEvent e) {
        int thisX = getLocation().x;
        int thisY = getLocation().y;
        int xMoved = (thisX + e.getX()) - (thisX + initialClick.x);
        int yMoved = (thisY + e.getY()) - (thisY + initialClick.y);
        int X = thisX + xMoved;
        X = Math.max(X, 10);
        X = (X + getWidth() > AppUtils.width) ? AppUtils.width - getWidth() - 10 : X;
        int Y = thisY + yMoved;
        Y = (Y + getHeight() > AppUtils.height) ? AppUtils.height - getHeight() - 10 : Y;
        Y = Math.max(Y, 10);
        setLocation(X, Y);
        AppUtils.LOCATION = getLocation();
    }

    private void checkUpdate() {
        i = 0;
        final String[] s = new String[] {".", "..", "...", "...."};
        Timer time =
                new Timer(
                        200, e -> lbStatus.setText("Đang kiểm tra cập nhật" + s[i = (i + 1) % 4]));
        time.start();

        PluginManager.getManager();
        // UpdateUI.checkUpdate();
        time.stop();
        timer.start();
    }

    private void createBlur() {
        Component root = getRootPane();
        blurBuffer = GraphicsUtils.createCompatibleImage(getWidth(), getHeight());
        Graphics2D g2 = blurBuffer.createGraphics();
        root.paint(g2);
        g2.dispose();

        backBuffer = blurBuffer;
        blurBuffer = GraphicsUtils.createThumbnailFast(blurBuffer, getWidth() / 2);
        blurBuffer = new GaussianBlurFilter(3).filter(blurBuffer, null);
        RescaleOp op = new RescaleOp(0.9f, 0, null);
        blurBuffer = op.filter(blurBuffer, null);
    }

    @Override
    public void setBlur(boolean blur) {
        if (blur) createBlur();
        else blurBuffer = null;
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (isVisible() && blurBuffer != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(backBuffer, 0, 0, null);
            g2.setComposite(AlphaComposite.SrcOver.derive(0.9f));
            g2.drawImage(blurBuffer, 0, 0, getWidth(), getHeight(), null);
            g2.dispose();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btMenu) {
            menu.show(btMenu, btMenu.getWidth() / 2 - 70, btMenu.getHeight() / 2 - 20);
        }
        if (e.getSource() == btExit) actionExit();

        if (e.getSource() == btAdd) {
            actionAdd();
        }

        if (e.getSource() == btBack) {
            Animation.go(setting, downloadUI);
        }
        if (e.getSource() == btOk) {
            setting.save();
            Animation.go(setting, downloadUI);
        }
    }
}
