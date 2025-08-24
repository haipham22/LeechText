package dark.leech.text.ui.main;

import dark.leech.text.image.ImageLabel;
import dark.leech.text.listeners.BlurListener;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.models.Properties;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.SyntaxDialog;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.button.SelectButton;
import dark.leech.text.ui.main.export.ExportEbook;
import dark.leech.text.ui.main.export.ExportText;
import dark.leech.text.ui.main.export.config.ConfigUI;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.ui.material.JMTextField;
import dark.leech.text.util.*;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class InfoUI extends JMDialog {
    private PanelTitle pnTitle;
    private JPanel pnCover;
    private CircleButton btEdit;
    private ImageLabel lbCover;
    private BasicButton btConfig;
    private BasicButton btText;
    private BasicButton btExport;
    private JLabel lbName;
    private JMTextField tfName;
    private JLabel lbAuthor;
    private JMTextField tfAuthor;
    private JLabel lbStatus;
    private GioiThieu gioiThieu;

    private Properties properties;

    public InfoUI(Properties properties) {
        this.properties = properties;
        setSize(340, 265);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onCreate();
            }
        });
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        pnTitle = new PanelTitle();
        pnCover = new JPanel();
        lbCover = new ImageLabel();
        btConfig = new BasicButton();
        btText = new BasicButton();
        btExport = new BasicButton();
        lbName = new JLabel();
        lbAuthor = new JLabel();
        lbStatus = new JLabel();
        gioiThieu = new GioiThieu(properties);


        pnTitle.setText(properties.getName());
        pnTitle.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        container.add(pnTitle);
        pnTitle.setBounds(0, 0, 340, 45);

        pnCover.setLayout(null);

        //---- btEdit ----
        btEdit = new CircleButton(StringUtils.EDIT, 16f);
        btEdit.setForeground(Color.BLACK);
        btEdit.setBackground(Color.darkGray);
        btEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileDialog file = new FileDialog((Frame) null, "Chọn ảnh", FileDialog.LOAD);
                file.setLocation(AppUtils.getLocation());
                file.setModal(true);
                file.setVisible(true);
                if (file.getFile() != null) {
                    FileUtils.copyFile(file.getDirectory() + file.getFile(), properties.getSavePath() + "/data/cover.jpg");
                    lbCover.path(properties.getSavePath() + "/data/cover.jpg")
                            .load();
                }
            }
        });
        pnCover.add(btEdit);
        btEdit.setBounds(70, 120, 30, 30);

        //---- lbCover ----
        pnCover.add(lbCover);
        lbCover.setBounds(0, 0, 100, 150);


        container.add(pnCover);
        pnCover.setBounds(5, 50, 100, 150);

        //---- btConfig ----
        btConfig.setText("Hiệu Chỉnh");
        btConfig.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                doConfig();
            }
        });
        container.add(btConfig);
        btConfig.setBounds(10, 215, 100, 35);

        //---- btText ----
        btText.setText("Xuất Text");
        btText.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                exportText();
            }
        });
        container.add(btText);
        btText.setBounds(115, 215, 100, 35);

        //---- btExport ----
        btExport.setText("Xuất Ebook");
        btExport.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                exportEbook();
            }
        });
        container.add(btExport);
        btExport.setBounds(225, 215, 100, 35);

        //---- lbName ----
        lbName.setText("Tên truyện");
        lbName.setFont(FontUtils.TEXT_NORMAL);
        container.add(lbName);
        lbName.setBounds(115, 50, 220, 20);
        tfName = new JMTextField();
        tfName.setText(properties.getName());
        tfName.setBounds(115, 75, 220, 30);
        container.add(tfName);


        //---- lbAuthor ----
        lbAuthor.setText("Tác giả");
        lbAuthor.setFont(FontUtils.TEXT_NORMAL);
        container.add(lbAuthor);
        lbAuthor.setBounds(115, 110, 220, 20);
        tfAuthor = new JMTextField();
        tfAuthor.setText(properties.getAuthor());
        tfAuthor.setBounds(115, 135, 220, 30);
        container.add(tfAuthor);
        //----GioiThieu----
        container.add(gioiThieu);
        gioiThieu.addBlurListener(this);
        gioiThieu.setBounds(115, 170, 220, 30);

        //---- lbStatus ----
        lbStatus.setFont(FontUtils.TEXT_NORMAL);
        container.add(lbStatus);
        lbStatus.setBounds(115, 170, 220, 30);
        lbCover.path(properties.getSavePath() + "/data/cover.jpg")
                .load();

    }

    private void doConfig() {
        properties.setName(tfName.getText());
        properties.setAuthor(tfAuthor.getText());
        ConfigUI configUI = new ConfigUI(properties);
        configUI.setBlurListener(this);
        configUI.open();
    }

    private void exportText() {
        properties.setName(tfName.getText());
        properties.setAuthor(tfAuthor.getText());
        ExportText export = new ExportText(properties);
        export.setBlurListener(this);
        export.open();
    }

    private void exportEbook() {
        properties.setName(tfName.getText());
        properties.setAuthor(tfAuthor.getText());
        ExportEbook export = new ExportEbook(properties);
        export.setBlurListener(this);
        export.open();
    }


}

class GioiThieu extends JMPanel {
    private JLabel lbName;
    private CircleButton btEdit;
    private SelectButton btSelect;
    private Properties properties;
    private BlurListener blurListener;

    public GioiThieu(Properties properties) {
        this.properties = properties;
        onCreate();
    }

    private void onCreate() {
        lbName = new JLabel();
        btSelect = new SelectButton();

        lbName.setText("Thêm trang giới thiệu");
        lbName.setFont(FontUtils.TEXT_NORMAL);
        add(lbName);
        lbName.setBounds(0, 0, 150, 30);

        btEdit = new CircleButton(StringUtils.EDIT);
        btEdit.setForeground(ColorUtils.THEME_COLOR);
        btEdit.setToolTipText("Sửa");

        btEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doEdit();
            }
        });
        add(btEdit);
        btEdit.setBounds(160, 0, 30, 30);
        btEdit.setVisible(false);
        btSelect.setSelected(false);
        add(btSelect);
        btSelect.setBounds(190, 0, 30, 30);
        btSelect.setChangeListener(new ChangeListener() {
            @Override
            public void doChanger() {
                GioiThieu.this.doChanger();
            }
        });
        setPreferredSize(new Dimension(300, 40));
    }

    private void doChanger() {
        btEdit.setVisible(btSelect.isSelected());
        properties.setAddGt(btSelect.isSelected());
        if (properties.isAddGt()) {
            File file = new File(properties.getSavePath() + "/raw/gioithieu.txt");
            if (!file.exists())
                FileUtils.string2file(properties.getGioiThieu(), properties.getSavePath() + "/raw/gioithieu.txt");
            else
                properties.setGioiThieu(FileUtils.file2string(properties.getSavePath() + "/raw/gioithieu.txt"));
        }
    }

    private void doEdit() {
        final SyntaxDialog editDialog = new SyntaxDialog("Giới thiệu", properties.getGioiThieu(), SyntaxConstants.SYNTAX_STYLE_HTML);
        editDialog.setBlurListener(blurListener);
        editDialog.setChangeListener(new ChangeListener() {
            @Override
            public void doChanger() {
                properties.setGioiThieu(editDialog.getText());
                if (properties.isAddGt())
                    FileUtils.string2file(properties.getGioiThieu(), properties.getSavePath() + "/raw/gioithieu.txt");
            }
        });
        editDialog.open();

    }

    public void addBlurListener(BlurListener blurListener) {
        this.blurListener = blurListener;
    }
}
