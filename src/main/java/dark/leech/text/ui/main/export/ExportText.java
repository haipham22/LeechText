package dark.leech.text.ui.main.export;

import dark.leech.text.action.export.Text;
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Long on 9/10/2016.
 */
public class ExportText extends JMDialog implements ProgressListener, ChangeListener {
    private PanelTitle pnTitle;
    private SelectBox sbStyle;
    private SelectBox sbType;
    private JMCheckBox cbToc;
    private JMCheckBox cbCss;
    private BasicButton btOk;
    private JMProgressBar progressBar;
    private Properties properties;

    public ExportText(Properties properties) {
        this.properties = properties;
        setSize(245, 255);
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

        //======== this ========
        pnTitle.setText("Xuất Text");
        pnTitle.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        container.add(pnTitle);
        pnTitle.setBounds(0, 0, 245, 45);

        //---- sbStyle ----
        sbStyle = new SelectBox("Định dạng xuất", new String[]{"HTML", "TXT"}, 0);
        sbStyle.addChangeListener(this);
        sbStyle.addBlurListener(this);
        container.add(sbStyle);
        sbStyle.setBounds(15, 60, 200, 30);


        //---- sbType ----
        sbType = new SelectBox("Kiểu xuất", new String[]{"Tách", "Gộp"}, 0);
        sbType.addBlurListener(this);
        sbType.addChangeListener(this);
        container.add(sbType);
        sbType.setBounds(15, 95, 200, 30);

        cbToc = new JMCheckBox("Đính kèm mục lục");
        container.add(cbToc);
        cbToc.setBounds(15, 130, 200, 30);
        cbToc.setVisible(sbStyle.getSelectIndex() == 0);

        cbCss = new JMCheckBox("Đính kèm CSS");
        container.add(cbCss);
        cbCss.setBounds(15, 165, 200, 30);
        cbCss.setVisible(sbStyle.getSelectIndex() == 0 && sbType.getSelectIndex() == 1);
        //---- btOk ----
        btOk.setText("XUẤT");
        btOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        doExport();
                    }
                }).start();
            }
        });
        container.add(btOk);
        btOk.setBounds(15, 210, 210, 35);
        progressBar.setPercent(0);
        container.add(progressBar);
        progressBar.setBounds(15, 210, 210, 35);
        progressBar.setVisible(false);

    }

    private void doExport() {
        progressBar.setVisible(true);
        btOk.setVisible(false);
        Text exportText = new Text(properties, sbStyle.getSelectIndex(), cbToc.isChecked(), cbCss.isChecked(), sbType.getSelectIndex());
        exportText.addProgressListener(this);
        exportText.export();

        Notification.build()
                .title(properties.getName())
                .content("Đã xuất text xong!")
                .path(properties.getSavePath() + "/data/cover.jpg")
                .delay(10000)
                .open();
    }

    @Override
    public void setProgress(int value, String string) {
        progressBar.setPercent(value);
        if (value == 100)
            close();
    }

    @Override
    public void doChanger() {
        cbToc.setVisible(sbStyle.getSelectIndex() == 0);
        cbCss.setVisible(sbStyle.getSelectIndex() == 0 && sbType.getSelectIndex() == 1);
    }
}
