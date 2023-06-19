package dark.leech.text.ui.main.export.config;

import dark.leech.text.action.Config;
import dark.leech.text.action.History;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.models.Chapter;
import dark.leech.text.models.Properties;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.util.FontUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Created by Long on 9/10/2016.
 */
public class ConfigUI extends JMDialog implements ChangeListener {

    public static final int NAME = 0, IMG = 1, ERROR = 2, OPTIMIZE = 3;
    private PanelTitle pnTitle;
    private BasicButton btOk;
    private JLabel lbName;
    private JLabel lbImg;
    private JLabel lbError;
    private BasicButton btName;
    private BasicButton btError;
    private BasicButton btImg;
    private BasicButton btList;
    protected Properties properties;
    private List<Chapter> chapList;
    private Config config;
    private List<Chapter> nameList;
    private List<Chapter> imgList;
    private List<Chapter> errorList;

    public ConfigUI(Properties properties) {
        this.properties = properties;
        this.chapList = properties.getChapList();
        setSize(295, 260);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onCreate();
                loadErr();
            }
        });

    }

    @Override
    protected void onCreate() {
        super.onCreate();
        pnTitle = new PanelTitle();
        btOk = new BasicButton();
        lbName = new JLabel();
        lbImg = new JLabel();
        lbError = new JLabel();
        btName = new BasicButton();
        btError = new BasicButton();
        btImg = new BasicButton();
        btList = new BasicButton();


        //======== pnTitle ========

        pnTitle.setText("Hiệu Chỉnh");
        pnTitle.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        container.add(pnTitle);
        pnTitle.setBounds(0, 0, 295, 45);

        //---- btOk ----
        btOk.setText("Hoàn Tất");
        btOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveProperties();
                close();
            }
        });
        container.add(btOk);
        btOk.setBounds(170, 210, 100, 35);

        //
        btList.setText("Xem DS");
        btList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Optimize();
            }
        });
        container.add(btList);
        btList.setBounds(10, 210, 100, 35);
        //---- lbName ----

        lbName.setFont(FontUtils.TEXT_NORMAL);
        container.add(lbName);
        lbName.setBounds(10, 60, 175, 35);

        //---- lbImg ----
        lbImg.setFont(FontUtils.TEXT_NORMAL);
        container.add(lbImg);
        lbImg.setBounds(10, 105, 175, 35);

        //---- lbError ----
        lbError.setFont(FontUtils.TEXT_NORMAL);
        container.add(lbError);
        lbError.setBounds(10, 150, 175, 35);

        //---- btName ----
        btName.setText("H.Chỉnh");
        btName.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editName();
            }
        });
        container.add(btName);
        btName.setBounds(185, 60, 95, 35);

        //---- btError ----
        btError.setText("H.Chỉnh");
        btError.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editError();
            }
        });
        container.add(btError);
        btError.setBounds(185, 150, 95, 35);

        //---- btImg ----
        btImg.setText("H.Chỉnh");
        btImg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editImg();
            }
        });
        container.add(btImg);
        btImg.setBounds(185, 105, 95, 35);


    }

    private void loadErr() {
        config = new Config(chapList);
        nameList = config.checkName();
        lbName.setText("Tên chương: " + Integer.toString(nameList.size()) + " không hợp lệ");
        if (nameList.size() == 0)
            btName.setVisible(false);

        imgList = config.checkImg();
        lbImg.setText("Chương ảnh: " + Integer.toString(imgList.size()) + " chương");
        if (imgList.size() == 0)
            btImg.setVisible(false);

        errorList = config.checkError();
        lbError.setText("Chương lỗi: " + Integer.toString(errorList.size()) + " chương");
        if (errorList.size() == 0)
            btError.setVisible(false);
    }

    private void editName() {
        ListUI listName = new ListUI(nameList, "Tên chương không hợp lệ", properties.getSavePath());
        listName.setBlurListener(this);
        listName.setAction(NAME);
        listName.open();
    }

    private void editImg() {
        ListUI listImg = new ListUI(imgList, "Chương ảnh", properties.getSavePath());
        listImg.setBlurListener(this);
        listImg.setAction(IMG);
        listImg.open();
    }

    private void editError() {
        ListUI listError = new ListUI(errorList, "Chương lỗi", properties.getSavePath());
        listError.setBlurListener(this);
        listError.setProperties(properties);
        listError.setAction(ERROR);
        listError.open();
    }

    private void Optimize() {
        ListUI list = new ListUI(chapList, "Danh sách chương", properties.getSavePath());
        list.setBlurListener(this);
        list.setAction(OPTIMIZE);
        list.open();
    }

    private void saveProperties() {
        properties.setSize(chapList.size());
        History.getHistory().save(properties);
    }

    @Override
    public void doChanger() {
        repaint();
    }
}

