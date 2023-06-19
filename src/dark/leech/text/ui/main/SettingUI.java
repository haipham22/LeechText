package dark.leech.text.ui.main;

import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMScrollPane;
import dark.leech.text.ui.notification.Toast;
import dark.leech.text.ui.setting.ItemConn;
import dark.leech.text.ui.setting.ItemStyle;
import dark.leech.text.ui.setting.Theme;
import dark.leech.text.ui.setting.ToolPane;
import dark.leech.text.ui.setting.trash.TrashPane;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static dark.leech.text.util.SettingUtils.*;

public class SettingUI extends JPanel {
    private JPanel body;
    private JMScrollPane scrollPane;
    // connection
    private ItemConn maxConn;
    private ItemConn reConn;
    private ItemConn timeConn;
    private ItemConn delayConn;
    private ItemConn userAgent;
    // style
    private ItemStyle htmlStyle;
    private ItemStyle txtStyle;
    private ItemStyle cssStyle;
    private ItemStyle dropStyle;
    // other
    private TrashPane trash;
    private ToolPane kindlegen;
    private ToolPane calibre;
    private ToolPane workPath;
    private Theme theme;

    private BasicButton defaultButton;

    public SettingUI() {
        setLayout(null);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                onCreate();
            }
        });
    }

    public void load() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateStatus();
            }
        }).start();
    }

    private void onCreate() {
        setBackground(Color.WHITE);
        body = new JPanel(new GridBagLayout());
        body.setBackground(Color.white);
        GridBagConstraints gi = new GridBagConstraints();
        gi.gridwidth = GridBagConstraints.REMAINDER;
        gi.weightx = 1;
        gi.weighty = 1;
        scrollPane = new JMScrollPane(body);

        JPanel demo = new JPanel();
        demo.setBackground(Color.WHITE);
        body.add(demo, gi);
        add(scrollPane);
        scrollPane.setBounds(0, 55, 390, 440);
        //
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        // body.
        maxConn = new ItemConn("Số kết nối tối đa", "5");
        reConn = new ItemConn("Số kết nối lại khi bị lỗi", "3");
        timeConn = new ItemConn("Thời gian kết nối tối đa", "10000");
        delayConn = new ItemConn("Thời gian chờ giữa các kết nối", "100");
        userAgent = new ItemConn("User Agent", "");

        htmlStyle = new ItemStyle("Tùy chỉnh HTML SYNTAX", "Tùy chỉnh cấu trúc HTML khi lưu", false);
        htmlStyle.setStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
        txtStyle = new ItemStyle("Tùy chỉnh TXT SYNTAX", "Tùy chỉnh cấu trúc TXT khi lưu", false);
        cssStyle = new ItemStyle("Tùy chỉnh CSS SYNTAX", "Tùy chỉnh cấu trúc CSS khi lưu", false);
        cssStyle.setStyle(SyntaxConstants.SYNTAX_STYLE_CSS);
        dropStyle = new ItemStyle("Tùy chỉnh DropCaps", "Tùy chỉnh cấu trúc DropCaps khi lưu", false);
        dropStyle.setStyle(SyntaxConstants.SYNTAX_STYLE_HTML);

        trash = new TrashPane(TRASH);
        kindlegen = new ToolPane("Đường dẫn Kindlegen", "", FileDialog.LOAD);
        calibre = new ToolPane("Đường dẫn Calibre", "", FileDialog.LOAD);
        workPath = new ToolPane("Thư mục lưu trữ", "", FileDialog.SAVE);
        workPath.setSelectDirectory(true);
        theme = new Theme(THEME_COLOR);

        body.add(theme, gbc, 0);
        body.add(calibre, gbc, 0);
        body.add(kindlegen, gbc, 0);
        body.add(workPath, gbc, 0);
        body.add(trash, gbc, 0);
        body.add(Label("Khác"), gbc, 0);
        body.add(cssStyle, gbc, 0);
        body.add(txtStyle, gbc, 0);
        body.add(htmlStyle, gbc, 0);
        body.add(dropStyle, gbc, 0);
        body.add(Label("Cấu trúc lưu"), gbc, 0);
        body.add(userAgent, gbc, 0);
        body.add(delayConn, gbc, 0);
        body.add(reConn, gbc, 0);
        body.add(timeConn, gbc, 0);
        body.add(maxConn, gbc, 0);
        body.add(Label("Kết nối"), gbc, 0);

        //
        defaultButton = new BasicButton();
        defaultButton.setText("Khôi phục mặc định");
        defaultButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doDefault();
                updateStatus();
                Toast.Build()
                        .font(FontUtils.TITLE_NORMAL)
                        .content("Đã khôi phục mặc định!")
                        .open();
            }
        });
        add(defaultButton);
        defaultButton.setBounds(0, 495, 390, 35);
        // repaint();
    }

    private JPanel Label(String name) {
        JPanel panel = new JPanel();
        JLabel label = new JLabel(name);
        label.setForeground(ColorUtils.THEME_COLOR.darker());
        label.setFont(FontUtils.TITLE_NORMAL);
        panel.add(label);
        label.setBounds(10, 0, 200, 30);
        panel.setLayout(null);
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(370, 30));
        return panel;
    }

    private void updateStatus() {
        maxConn.setText(toString(MAX_CONN));
        reConn.setText(toString(RE_CONN));
        timeConn.setText(toString(TIMEOUT));
        delayConn.setText(toString(DELAY));
        userAgent.setText(USER_AGENT);
        //
        htmlStyle.setSelected(IS_HTML_SELECTED);
        htmlStyle.setText(HTML_SYNTAX);
        txtStyle.setSelected(IS_TXT_SELECTED);
        txtStyle.setText(TXT_SYNTAX);
        cssStyle.setSelected(IS_CSS_SELECTED);
        cssStyle.setText(CSS_SYNTAX);
        dropStyle.setSelected(IS_DROP_SELECTED);
        dropStyle.setText(DROP_SYNTAX);
        //
        trash.setTrash(TRASH);
        workPath.setPath(WORKPATH);
        calibre.setPath(CALIBRE);
        kindlegen.setPath(KINDLEGEN);
        theme.setThemeColor(THEME_COLOR);
    }


    public void save() {
        MAX_CONN = toInt(maxConn.getText());
        RE_CONN = toInt(reConn.getText());
        TIMEOUT = toInt(timeConn.getText());
        DELAY = toInt(delayConn.getText());
        USER_AGENT = userAgent.getText();


        IS_DROP_SELECTED = dropStyle.isSelected();
        IS_HTML_SELECTED = htmlStyle.isSelected();
        IS_TXT_SELECTED = txtStyle.isSelected();
        IS_CSS_SELECTED = cssStyle.isSelected();
        DROP_SYNTAX = dropStyle.getText();
        HTML_SYNTAX = htmlStyle.getText();
        TXT_SYNTAX = txtStyle.getText();
        CSS_SYNTAX = cssStyle.getText();

        TRASH = trash.getTrash();
        WORKPATH = workPath.getPath();
        CALIBRE = calibre.getPath();
        KINDLEGEN = kindlegen.getPath();
        THEME_COLOR = theme.getColor();
        doSave();
    }

    private String toString(int i) {
        return Integer.toString(i);
    }

    private int toInt(String s) {
        return Integer.parseInt(s);
    }
}
