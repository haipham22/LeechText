package dark.leech.text.ui.main.export;

import dark.leech.text.action.export.Ebook;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.listeners.ProgressListener;
import dark.leech.text.models.Properties;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMCheckBox;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMProgressBar;
import dark.leech.text.ui.material.SelectBox;
import dark.leech.text.ui.notification.Notification;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.TypeUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Long on 9/10/2016.
 */
public class ExportEbook extends JMDialog implements ProgressListener {

    private PanelTitle pnTitle;
    private SelectBox pnTool;
    private SelectBox pnType;
    private SelectBox pnComp;
    private JMCheckBox cbSplit;
    private JMCheckBox cbInclude;
    private BasicButton btOk;
    private JMProgressBar progressBar;
    private JLabel labelProgress;
    private Properties properties;

    public ExportEbook(Properties properties) {
        this.properties = properties;
        setSize(245, 300);
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
        btOk = new BasicButton();
        progressBar = new JMProgressBar();
        labelProgress = new JLabel();

        pnTitle.setText("Tạo Ebook");
        pnTitle.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        container.add(pnTitle);
        pnTitle.setBounds(0, 0, 245, 45);

        //---- pnType2 ----

        pnTool = new SelectBox("Công cụ", new String[]{"Mặc định", "Calibre"}, 0);
        pnTool.addChangeListener(new ChangeListener() {
            @Override
            public void doChanger() {
                ToolChanger();
            }
        });
        pnTool.addBlurListener(this);
        container.add(pnTool);
        pnTool.setBounds(10, 90, 215, 30);

        //---- pnType ----
        pnType = new SelectBox("Định dạng xuất", new String[]{"EPUB", "MOBI", "AZW3", "PDF"}, 0);
        pnType.addChangeListener(new ChangeListener() {
            @Override
            public void doChanger() {
                TypeChanger();
                ToolChanger();
            }
        });
        pnType.addBlurListener(this);
        container.add(pnType);
        pnType.setBounds(10, 55, 215, 30);


        //---- pnType3 ----
        pnComp = new SelectBox("Mức nén", new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"}, 8);
        pnComp.addBlurListener(this);
        container.add(pnComp);
        pnComp.setBounds(10, 125, 215, 30);

        //---- checkBox1 ----
        cbSplit = new JMCheckBox("Tự động chia quyển");
        container.add(cbSplit);
        cbSplit.setBounds(10, 160, 215, 30);

        //
        cbInclude = new JMCheckBox("Chèn chương ảnh");
        container.add(cbInclude);
        cbInclude.setBounds(10, 195, 215, 30);
//
        labelProgress.setText("Đang xử lý...");
        labelProgress.setFont(FontUtils.TEXT_THIN);
        container.add(labelProgress);
        labelProgress.setBounds(15, 225, 210, 25);
        labelProgress.setVisible(false);
        //---- btOk ----
        btOk.setText("OK");
        btOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doClick();
            }
        });
        container.add(btOk);
        btOk.setBounds(15, 250, 210, 35);
        container.add(progressBar);
        progressBar.setBounds(15, 250, 210, 35);
        progressBar.setVisible(false);

    }

    private void doClick() {
        btOk.setVisible(false);
        progressBar.setVisible(true);
        labelProgress.setVisible(true);
        final Ebook ebook = new Ebook(properties);
        ebook.addProgressListener(this);
        ebook.setData(pnType.getSelectIndex(), pnTool.getSelectText(), pnComp.getSelectText(), cbSplit.isChecked(), true);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ebook.export();
                AppUtils.pause(200);
                btOk.setVisible(true);
                progressBar.setVisible(false);
                progressBar.setPercent(0);
                labelProgress.setVisible(false);
                labelProgress.setText("Đang xử lý...");
            }
        });
    }

    private void TypeChanger() {
        switch (pnType.getSelectIndex()) {
            case TypeUtils.EPUB:
                pnTool.setModel(new String[]{"Mặc định", "Calibre"}, 0);
                break;
            case TypeUtils.AZW3:
            case TypeUtils.DOC:
            case TypeUtils.PDF:
                pnTool.setModel(new String[]{"Calibre"}, 0);
                break;
            case TypeUtils.MOBI:
                pnTool.setModel(new String[]{"Calibre", "Kindlegen"}, 0);
                break;
        }
    }

    private void ToolChanger() {
        String toolName = pnTool.getSelectText();
        if (toolName.equals("Mặc định")) {
            pnComp.setVisible(true);
            pnComp.setModel(new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9"}, 8);
        }
        if (toolName.equals("Calibre"))
            switch (pnType.getSelectIndex()) {
                case TypeUtils.EPUB:
                case TypeUtils.AZW3:
                    pnComp.setVisible(false);
                    break;
                case TypeUtils.MOBI:
                    pnComp.setVisible(true);
                    pnComp.setModel(new String[]{"old", "both", "new"}, 1);
                    break;
                case TypeUtils.PDF:
                    pnComp.setVisible(true);
                    pnComp.setModel(new String[]{"a4", "a5", "a6"}, 0);
                    break;
            }
        if (toolName.equals("Kindlegen"))
            switch (pnType.getSelectIndex()) {
                case TypeUtils.MOBI:
                    pnComp.setModel(new String[]{"-c0", "-c1", "-c2"}, 2);
            }
    }

    @Override
    public void setProgress(int value, String string) {
        progressBar.setPercent(value);
        labelProgress.setText(string);
        if (string.equals("Hoàn tất!")) {
            Notification.build()
                    .title(properties.getName())
                    .content("Đã tạo ebook xong!")
                    .path(properties.getSavePath() + "/data/cover.jpg")
                    .delay(10000)
                    .open();
            close();
        }
    }
}
