package dark.leech.text.ui.download;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.get.InfoExecute;
import dark.leech.text.get.ListExecute;
import dark.leech.text.listeners.AddListener;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.models.Chapter;
import dark.leech.text.models.Pager;
import dark.leech.text.models.Properties;
import dark.leech.text.plugin.PluginManager;
import dark.leech.text.ui.CircleWait;
import dark.leech.text.ui.ListDialog;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMTextField;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.SettingUtils;
import dark.leech.text.util.SyntaxUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;


public class AddDialog extends JMDialog {

    private JLabel lbName;
    private JLabel lbAuthor;
    private JLabel lbChap;
    private JMTextField tfName;
    private JMTextField tfAuthor;
    private JMTextField tfChap;
    private BasicButton btShowChap;
    private BasicButton btOk;
    private BasicButton btCancel;


    private CircleWait circleWait;
    private Properties properties;
    private AddListener addListener;
    private PluginEntity plugin;
    private String url;
    private boolean doImp;

    public AddDialog(String url) {
        this.url = url;
        onCreate();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doLoad();
            }
        });
    }


    public AddDialog(Properties properties) {
        this.properties = properties;
        onCreate();
        tfName.setEnabled(true);
        tfName.setText(properties.getName());
        tfAuthor.setEnabled(true);
        tfAuthor.setText(properties.getAuthor());
        btShowChap.setVisible(true);
        tfChap.setEnabled(true);
        tfChap.setText("1-" + Integer.toString(properties.getSize()));
        btShowChap.setVisible(true);
        btOk.setEnabled(true);
        doImp = true;
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        lbName = new JLabel();
        lbAuthor = new JLabel();
        lbChap = new JLabel();
        tfName = new JMTextField();
        tfAuthor = new JMTextField();
        tfChap = new JMTextField();
        circleWait = new CircleWait(getPreferredSize());
        JLayer<JPanel> layer = circleWait.getJlayer();
        container.add(layer);
        layer.setBounds(0, 0, 300, 260);
        // ---- button1 ----
        btOk = new BasicButton();
        btOk.setText("OK");
        btOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionAdd();
            }
        });
        container.add(btOk);
        btOk.setEnabled(false);
        btOk.setBounds(10, 210, 110, 35);

        // ---- button2 ----
        btCancel = new BasicButton();
        btCancel.setText("HỦY");
        btCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        container.add(btCancel);
        btCancel.setBounds(180, 210, 110, 35);

        lbName.setText("Tên truyện");
        lbName.setFont(FontUtils.TEXT_NORMAL);
        container.add(lbName);
        lbName.setBounds(10, 10, 280, 25);
        tfName.setEnabled(false);
        tfName.setBounds(10, 35, 280, 37);
        container.add(tfName);

        lbAuthor.setText("Tác giả");
        lbAuthor.setFont(FontUtils.TEXT_NORMAL);
        container.add(lbAuthor);
        lbAuthor.setBounds(10, 75, 280, 25);
        tfAuthor.setEnabled(false);
        tfAuthor.setBounds(10, 100, 280, 37);
        container.add(tfAuthor);

        lbChap.setText("Danh sách chương");
        lbChap.setFont(FontUtils.TEXT_NORMAL);
        container.add(lbChap);
        lbChap.setBounds(10, 140, 280, 25);
        tfChap.setEnabled(false);
        tfChap.setBounds(10, 165, 200, 37);
        container.add(tfChap);
        btShowChap = new BasicButton();
        btShowChap.setText("Xem DS");
        btShowChap.setBounds(220, 165, 70, 37);
        btShowChap.setVisible(false);
        btShowChap.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionShow();
            }
        });
        container.add(btShowChap);
        this.setSize(300, 260);
    }

    public Properties getProperties() {
        return properties;
    }

    private void doLoad() {
        PluginManager pluginManager = PluginManager.getManager();

        plugin = pluginManager.get(url);

        //Bắt đầu lấy thông tin
        circleWait.startWait();
        properties = new Properties();
        properties.setUrl(url);
        InfoExecute getInfo = new InfoExecute();
        final ListExecute getList = new ListExecute();

        getList.plugin(plugin)
                .listener(new ChangeListener() {
                    @Override
                    public void doChanger() {
                        tfChap.setEnabled(true);
                        tfChap.setText("1-" + Integer.toString(properties.getSize()));
                        btShowChap.setVisible(true);
                        btOk.setEnabled(true);
                        circleWait.stopWait();
                    }
                })
                .applyTo(properties);
        getInfo.plugin(plugin)
                .listener(new ChangeListener() {
                    @Override
                    public void doChanger() {
                        tfName.setEnabled(true);
                        tfName.setText(properties.getName());
                        tfAuthor.setEnabled(true);
                        tfAuthor.setText(properties.getAuthor());
                        getList.execute();
                    }
                })
                .applyTo(properties)
                .execute();

    }

    private void actionAdd() {
        if (doImp) {
            addListener.addDownload(properties, doImp);
        } else {
            properties.setName(SyntaxUtils.covertString(tfName.getText().trim()));
            properties.setAuthor(SyntaxUtils.covertString(tfAuthor.getText().trim()));
            if (properties.getName().length() == 0) {
                tfName.addError("Tên truyện không được để trống!");
                return;
            }
            if (properties.getAuthor().length() == 0) {
                tfAuthor.addError("Tên tác giả không được để trống");
                return;
            }
            String savePath = SyntaxUtils.xoaDau(tfName.getText());
            savePath = savePath.replaceAll("[^a-zA-Z0-9_]", "_")
                    .replace("\"", "")
                    .trim();
            savePath = FileUtils.validate(SettingUtils.WORKPATH + "/output/" + savePath);
            properties.setSavePath(savePath);
            properties.setUrl(url);
            FileUtils.mkdir(savePath);
            if (parseListChap())
                addListener.addDownload(properties, false);
            else return;
        }
        close();
    }

    private void actionShow() {
        final ListDialog list;
        if (properties.isForum())
            list = new ListDialog(properties.getPageList(), tfChap.getText(), true);
        else
            list = new ListDialog(properties.getChapList(), tfChap.getText());
        list.setBlurListener(this);
        list.setChangeListener(new ChangeListener() {
            @Override
            public void doChanger() {
                tfChap.setText(list.getParseList());
            }
        });
        list.open();

    }

    private boolean parseListChap() {
        int size = 0;
        List<Pager> pageList = properties.getPageList();
        List<Chapter> chapList = properties.getChapList();

        List<Pager> newPageList = new ArrayList<>();
        List<Chapter> newChapList = new ArrayList<>();
        try {
            String[] list = tfChap.getText().replaceAll("\\s+", "").split(",");
            for (int i = 0; i < list.length; i++) {
                String[] c = list[i].split("-");
                int c0 = Integer.parseInt(c[0]);
                if (c.length == 1) {
                    if (properties.isForum())
                        newPageList.add(pageList.get(c0 - 1));
                    else newChapList.add(chapList.get(c0 - 1));
                    size++;
                    continue;
                }
                for (int j = c0; j <= Integer.parseInt(c[1]); j++) {
                    if (properties.isForum())
                        newPageList.add(pageList.get(j - 1));
                    else newChapList.add(chapList.get(j - 1));
                    size++;
                }
            }
        } catch (Exception e) {
            tfChap.addError("Lỗi tạo danh sách chương");
            return false;
        }
        properties.setPageList(newPageList);
        properties.setChapList(newChapList);
        properties.setSize(size);
        return true;
    }


    public void setAddListener(AddListener addListener) {
        this.addListener = addListener;
    }

}