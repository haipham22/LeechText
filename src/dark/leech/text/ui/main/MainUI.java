package dark.leech.text.ui.main;

import dark.leech.text.image.GaussianBlurFilter;
import dark.leech.text.listeners.BlurListener;
import dark.leech.text.plugin.PluginManager;
import dark.leech.text.ui.Animation;
import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.button.CloseButton;
import dark.leech.text.ui.download.AddURL;
import dark.leech.text.ui.download.DownloadUI;
import dark.leech.text.ui.main.plugin.PluginUI;
import dark.leech.text.ui.main.repository.RepositoryUI;
import dark.leech.text.ui.material.JMMenuItem;
import dark.leech.text.ui.material.JMPopupMenu;
import dark.leech.text.util.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainUI extends JFrame implements BlurListener, ActionListener {

    // global pn
    private DownloadUI downloadUI;
    private SettingUI setting;
    private JPanel appBar;
    private CircleButton btAdd;
    private CircleButton btMenu;
    // header pn
    private JPanel statusBar;
    private CircleButton btBack;
    private CircleButton btOk;
    private JLabel lbLogo;
    private JPanel pnHeader;
    // Menu
    private JMPopupMenu menu;
    private JMMenuItem pnSetting;
    private JMenuItem pnHelp;
    private JMMenuItem pnPlugin;
    private JMMenuItem pnRepository;

    //
    private CloseButton btExit;
    private JLabel lbStatus;

    private Container container;
    private Point initialClick;
    private BufferedImage blurBuffer;
    private BufferedImage backBuffer;
    private Timer timer;
    private int i = 0;

    public MainUI() {
        setLocation(AppUtils.width - 420, AppUtils.height - 650);
        setSize(390, 555);
        getRootPane().setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
        setUndecorated(true);
        setTitle("LeechText");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/dark/leech/res/icon.png")));
        new Thread(new Runnable() {
            @Override
            public void run() {
                onCreate();
            }
        }).start();

    }

    private void onCreate() {
        container = getContentPane();
        container.setLayout(null);
        container.setBackground(Color.WHITE);
        onCreateStatusBar();
        onCreateAppBar();
        createPanelHeaderUI();
        createPopupMenu();
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

    private void actionExit() {
        Animation.fadeOut(this);
    }

    private void actionAdd() {
        AddURL addURL = new AddURL();
        addURL.setAddListener(downloadUI);
        addURL.open();
    }

    //Thanh tiêu đề
    private void onCreateStatusBar() {
        statusBar = new JPanel();
        statusBar.setBackground(ColorUtils.STATUS_BAR);
        statusBar.setLayout(null);
        // ---- btExit ----

        btExit = new CloseButton();
        btExit.setFont(FontUtils.iconFont(18f));
        btExit.addActionListener(this);
        statusBar.add(btExit);
        btExit.setBounds(360, 0, 20, 20);
        // ---- lbStatus ----
        lbStatus = new JLabel();
        lbStatus.setForeground(Color.white);
        lbStatus.setFocusable(false);
        lbStatus.setFont(FontUtils.textFont(13f, Font.PLAIN));
        statusBar.add(lbStatus);
        lbStatus.setBounds(5, 0, 315, 20);
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DateFormat dateFormat = new SimpleDateFormat("HH:mm");
                Date date = new Date();
                lbStatus.setText(dateFormat.format(date));
            }
        });
        container.add(statusBar);
        statusBar.setBounds(0, 0, 390, 20);
        statusBar.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                initialClick = e.getPoint();
                getComponentAt(initialClick);
            }
        });
        statusBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                movieWindows(e);
            }
        });
    }

    //Thanh Title
    private void onCreateAppBar() {

        appBar = new JPanel();
        appBar.setBackground(ColorUtils.THEME_COLOR);
        appBar.setLayout(null);

        btAdd = new CircleButton(StringUtils.ADD, 25f);
        btAdd.addActionListener(this);
        appBar.add(btAdd);
        btAdd.setBounds(305, 5, 45, 45);
        // ---- logo ----
        JLabel logo;
        logo = new JLabel();
        logo.setText("Leech Text");
        logo.setFont(FontUtils.TITLE_BIG);
        logo.setForeground(Color.white);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        appBar.add(logo);
        logo.setBounds(20, 0, logo.getPreferredSize().width, 55);

        btMenu = new CircleButton(StringUtils.MORE, 25f);
        btMenu.addActionListener(this);
        appBar.add(btMenu);
        btMenu.setBounds(355, 5, 30, 45);

    }

    private void createPanelHeaderUI() {
        pnHeader = new JPanel();
        pnHeader.setBackground(ColorUtils.THEME_COLOR);
        pnHeader.setLayout(null);

        btBack = new CircleButton(StringUtils.BACK, 25f);
        btBack.addActionListener(this);
        pnHeader.add(btBack);
        btBack.setBounds(5, 5, 45, 45);
        // ---- logo ----
        lbLogo = new JLabel();
        lbLogo.setText("Cài đặt");
        lbLogo.setFont(FontUtils.TITLE_BIG);
        lbLogo.setForeground(Color.white);
        lbLogo.setHorizontalAlignment(SwingConstants.CENTER);
        pnHeader.add(lbLogo);
        lbLogo.setBounds(55, 0, 100, 55);

        btOk = new CircleButton(StringUtils.CHECK, 25f);
        btOk.addActionListener(this);
        pnHeader.add(btOk);
        btOk.setBounds(335, 5, 45, 45);
    }

    private void createPopupMenu() {
        ActionListener actionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == pnSetting) {
                    Animation.go(downloadUI, setting);
                    setting.load();
                } else if (e.getSource() == pnHelp) {
                    new HelpUI().open();
                } else if (e.getSource() == pnPlugin) {
                    new PluginUI().open();
                } else if (e.getSource() == pnRepository) {
                    new RepositoryUI().open();
                }
            }
        };
        menu = new JMPopupMenu();

        pnSetting = new JMMenuItem("Cài đặt");
        pnSetting.addActionListener(actionListener);

        pnPlugin = new JMMenuItem("Plugins");
        pnPlugin.addActionListener(actionListener);

        pnRepository = new JMMenuItem("Repository");
        pnRepository.addActionListener(actionListener);

        pnHelp = new JMMenuItem("Thông tin");
        pnHelp.addActionListener(actionListener);

        menu.add(pnSetting);
        menu.add(pnPlugin);
        menu.add(pnRepository);
        menu.add(pnHelp);

        container.add(menu);
    }

    private void movieWindows(MouseEvent e) {
        int thisX = getLocation().x;
        int thisY = getLocation().y;
        int xMoved = (thisX + e.getX()) - (thisX + initialClick.x);
        int yMoved = (thisY + e.getY()) - (thisY + initialClick.y);
        int X = thisX + xMoved;
        X = X < 10 ? 10 : X;
        X = (X + getWidth() > AppUtils.width) ? AppUtils.width - getWidth() - 10 : X;
        int Y = thisY + yMoved;
        Y = (Y + getHeight() > AppUtils.height) ? AppUtils.height - getHeight() - 10 : Y;
        Y = Y < 10 ? 10 : Y;
        setLocation(X, Y);
        AppUtils.LOCATION = getLocation();
    }

    private void checkUpdate() {
        i = 0;
        final String[] s = new String[]{".", "..", "...", "...."};
        Timer time = new Timer(200, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                lbStatus.setText("Đang kiểm tra cập nhật" + s[i = (i + 1) % 4]);
            }
        });
        time.start();
        PluginManager.getManager();
//        UpdateUI.checkUpdate();
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
        if (blur)
            createBlur();
        else
            blurBuffer = null;
        repaint();
    }


    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (isVisible() && blurBuffer != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
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
        if (e.getSource() == btExit)
            actionExit();

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
