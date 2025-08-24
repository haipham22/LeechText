package dark.leech.text.ui.download;

import dark.leech.text.action.Download;
import dark.leech.text.action.History;
import dark.leech.text.image.ImageLabel;
import dark.leech.text.listeners.DownloadListener;
import dark.leech.text.listeners.RemoveListener;
import dark.leech.text.models.Chapter;
import dark.leech.text.models.Pager;
import dark.leech.text.models.Properties;
import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.main.InfoUI;
import dark.leech.text.ui.material.DropShadowBorder;
import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.ui.material.JMProgressBar;
import dark.leech.text.ui.notification.Notification;
import dark.leech.text.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

public class DownloadLabel extends JMPanel implements DownloadListener {
    private JMProgressBar load;
    private JLabel lbName;
    private JLabel lbStatus;
    private JLabel lbPercent;
    private JLabel lbProgress;
    private ImageLabel pnCover;
    private CircleButton btDelete;
    private CircleButton btPR;
    private CircleButton btInfo;
    private Properties properties;
    private Download download;
    private RemoveListener removeListener;
    private boolean pause;
    private int status;
    private int max;

    public DownloadLabel() {
        onCreate();
        pause = false;
    }


    public void importDownload(Properties properties) {
        this.properties = properties;
        this.max = properties.getSize();
        btPR.setVisible(false);
        btInfo.setVisible(true);

        if (FontUtils.TEXT_BOLD.canDisplayUpTo(properties.getName()) == -1)
            lbName.setFont(FontUtils.TEXT_BOLD);
        lbName.setText(properties.getName());
        lbStatus.setText("Hoàn tất");
        setValue(max);
        pnCover.path(properties.getSavePath() + "/data/cover.jpg").load();

    }

    public void addDownload(Properties properties) {
        this.properties = properties;
        this.max = properties.getSize();
        doCreateFolder();
        download = new Download(properties);
        download.addDownloadListener(this);
        download.startDownload();
        if (FontUtils.TEXT_BOLD.canDisplayUpTo(properties.getName()) == -1)
            lbName.setFont(FontUtils.TEXT_BOLD);
        lbName.setText(properties.getName());

        pnCover.url(properties.getCover())
                .path(properties.getSavePath() + "/data/cover.jpg")
                .load();

    }

    public void addRemoveListener(RemoveListener removeListener) {
        this.removeListener = removeListener;
    }

    // Tạm dừng
    private void pause() {
        download.pause();
    }

    // Tiếp tục
    @SuppressWarnings("unused")
    private void resume() {
        download.resume();
    }

    // Xóa
    @SuppressWarnings("unused")
    private void delete() {
        if (download != null)
            download.cancel();
        removeListener.removeComponent(this);
    }

    // Kết thúc quá trình
    private void doAfterFinish() {
        btPR.setVisible(false);
        btInfo.setVisible(true);
        if (properties.isForum()) {
            ArrayList<Chapter> chapList = new ArrayList<>();
            for (Pager pager : properties.getPageList()) {
                for (Chapter ch : pager.getChapter())
                    chapList.add(ch);
            }
            properties.setChapList(chapList);
            properties.setSize(chapList.size());
        }
        History.getHistory().save(properties);
        Notification.build()
                .title(properties.getName())
                .content("Đã gettext xong!")
                .path(properties.getSavePath() + "/data/cover.jpg")
                .delay(5000)
                .open();

    }

    // Khi bị lỗi

    private void doAfterError() {
        pause();
        btInfo.setVisible(true);
    }

    // get dữ liệu đầu vào
    public Properties getProperties() {
        return properties;
    }

    // Đặt giá trị
    private void setValue(int value) {
        load.setPercent(value * 100 / max);
        lbProgress.setText(Integer.toString(value) + "/" + Integer.toString(max));
        lbPercent.setText(Integer.toString(load.getPercent()) + "%");
    }


    // Trạng thái
    public String getStatus(int status) {
        switch (status) {
            case Download.DOWNLOADING:
                btPR.setText(StringUtils.PAUSE);
                return "Đang tải...";
            case Download.PAUSE:
                btPR.setText(StringUtils.PLAY);
                return "Tạm dừng";
            case Download.ERROR:
                doAfterError();
                return "Lỗi";
            case Download.COMPLETED:
                doAfterFinish();
                return "Hoàn tất";
            case Download.CHECKING:
                return "Kiểm tra";
            default:
                return "";
        }
    }

    private void onCreate() {
        pnCover = new ImageLabel();
        load = new JMProgressBar();
        lbName = new JLabel();
        lbStatus = new JLabel();
        lbPercent = new JLabel();
        lbProgress = new JLabel();

        // ======== pnCover ========
        add(pnCover);
        pnCover.setBounds(5, 5, 55, 80);
        // ======== doLoad ========
        load.setOpaque(true);
        load.setLayout(null);
        add(load);
        load.setBounds(65, 70, 305, 4);
        // ---- lbName ----
        lbName.setForeground(ColorUtils.THEME_COLOR);
        add(lbName);
        lbName.setBounds(65, 5, 230, 30);

        // ---- lbStatus ----
        lbStatus.setText("Đang tải...");
        lbStatus.setFont(FontUtils.TEXT_NORMAL);
        lbStatus.setForeground(ColorUtils.THEME_COLOR);
        lbStatus.setHorizontalAlignment(SwingConstants.LEFT);
        add(lbStatus);
        lbStatus.setBounds(65, 40, 95, lbStatus.getPreferredSize().height);

        // ---- lbPercent ----
        lbPercent.setFont(FontUtils.TEXT_NORMAL);
        lbPercent.setHorizontalAlignment(SwingConstants.RIGHT);
        lbPercent.setForeground(ColorUtils.THEME_COLOR);
        add(lbPercent);
        lbPercent.setBounds(330, 40, 40, 30);

        // ---- lbProgress ----
        lbProgress.setHorizontalAlignment(SwingConstants.RIGHT);
        lbProgress.setFont(FontUtils.TEXT_NORMAL);
        lbProgress.setForeground(ColorUtils.THEME_COLOR);
        add(lbProgress);
        lbProgress.setBounds(200, 40, 90, 30);

        // ---- btPR ----
        btPR = new CircleButton("\ue034", 20f);
        btPR.setForeground(ColorUtils.THEME_COLOR);
        btPR.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pause = !pause;
                if (pause)
                    pause();
                else
                    resume();
            }
        });
        add(btPR);
        btPR.setBounds(310, 5, 30, 30);

        // ---- btInfo ----
        btInfo = new CircleButton(StringUtils.ADD, 20f);
        btInfo.setForeground(ColorUtils.THEME_COLOR);
        btInfo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new InfoUI(properties).open();
            }
        });
        add(btInfo);
        btInfo.setBounds(310, 5, 30, 30);
        btInfo.setVisible(false);
        // ---- btDelete ----
        btDelete = new CircleButton(StringUtils.DELETE, 20f);
        btDelete.setForeground(ColorUtils.THEME_COLOR);
        btDelete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                delete();
            }
        });
        add(btDelete);
        btDelete.setBounds(345, 5, 30, 30);
        setBorder(new DropShadowBorder(SettingUtils.THEME_COLOR, 5, 3));
        setPreferredSize(new Dimension(375, 90));
    }


    private void doCreateFolder() {
        FileUtils.mkdir(properties.getSavePath() + "/raw");
        FileUtils.mkdir(properties.getSavePath() + "/data");
        FileUtils.mkdir(properties.getSavePath() + "/data/Images");
        FileUtils.mkdir(properties.getSavePath() + "/data/Text");
        FileUtils.mkdir(properties.getSavePath() + "/out");
    }

    protected void runOnUiThread(Runnable runnable) {
        new Thread(runnable).start();
    }

    @Override
    public void updateDownload(int downloaded, int status) {
        setValue(downloaded);
        if (status != this.status) {
            this.status = status;
            lbStatus.setText(getStatus(status));
        }
    }

}
