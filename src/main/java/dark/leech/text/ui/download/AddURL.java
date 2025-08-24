package dark.leech.text.ui.download;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.listeners.AddListener;
import dark.leech.text.plugin.PluginManager;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMTextField;
import dark.leech.text.util.CookiesUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.SettingUtils;
import dark.leech.text.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AddURL extends JMDialog {

    private BasicButton btOk;
    private BasicButton btCancel;
    private JMTextField tfUrl;
    private CircleButton btAddMul;
    private JLabel lbUrl;
    private String url;
    private AddListener addListener;
    private String cookies;

    public AddURL() {
        onCreate();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        btOk = new BasicButton();
        btCancel = new BasicButton();
        btAddMul = new CircleButton(StringUtils.ADD);
        tfUrl = new JMTextField();
        lbUrl = new JLabel();
        // ---- button1 ----
        btOk.setText("OK");
        btOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                url = tfUrl.getText();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkURL();
                    }
                });
            }
        });

        container.add(btOk);
        btOk.setBounds(15, 75, 110, 35);

        // ---- button2 ----
        btCancel.setText("HỦY");
        btCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        container.add(btCancel);
        btCancel.setBounds(125, 75, 110, 35);

        tfUrl.setText(getClipboard());
        tfUrl.setFont(FontUtils.TEXT_NORMAL);
        tfUrl.setBounds(15, 30, 220, 37);

        container.add(tfUrl);

        // ---- lbUrl ----
        lbUrl.setText("Nhập URL");
        lbUrl.setFont(FontUtils.TEXT_BOLD);
        container.add(lbUrl);
        lbUrl.setBounds(15, 0, 110, 25);

        btAddMul.setBounds(220, 5, 25, 25);
        btAddMul.setForeground(SettingUtils.THEME_COLOR);
        btAddMul.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (tfUrl.getText() == null || tfUrl.getText().length() < 5)
                    tfUrl.addError("Xin nhập URL!");
                else {
                    AddOption lg = new AddOption();
                    lg.setBlurListener(AddURL.this);
                    lg.open();
                }
            }
        });
        container.add(btAddMul);
        container.setBackground(Color.WHITE);
        setSize(255, 125);
    }

    public String getUrl() {
        if (url.lastIndexOf("/") == url.length() - 1)
            url = url.substring(0, url.length() - 1);
        return url;
    }

    private void checkURL() {
        PluginEntity pluginGetter = PluginManager.getManager().get(url);
        if (pluginGetter == null)
            tfUrl.addError("Liên kết này không được hỗ trợ!");
        else if (pluginGetter.isChecked()) {
            AddDialog add = new AddDialog(getUrl());
            add.setAddListener(addListener);
            if (cookies != null && cookies.length() != 0)
                CookiesUtils.put(getUrl(), cookies);
            close();
            add.open();
        } else {
            tfUrl.addError("Plugin cho trang này đã bị tắt!");
        }
    }

    private String getClipboard() {
        try {
            Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String result = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                if (result.toLowerCase().startsWith("http"))
                    return result;
            }
        } catch (Exception e) {
        }
        return "";
    }

    public void setAddListener(AddListener addListener) {
        this.addListener = addListener;
    }

    class AddOption extends JMDialog {
        private BasicButton btOk;
        private BasicButton btCancel;
        private JMTextField tfCookies;

        public AddOption() {
            setSize(300, 100);
            onCreate();
        }

        @Override
        protected void onCreate() {
            super.onCreate();
            btOk = new BasicButton();
            btCancel = new BasicButton();
            tfCookies = new JMTextField();

            JLabel label = new JLabel("Cookie");
            label.setBounds(10, 10, 50, 35);
            tfCookies.setBounds(60, 10, 230, 35);
            btOk.setText("OK");
            btOk.setBounds(10, 55, 100, 35);
            btCancel.setText("HỦY");
            btCancel.setBounds(180, 55, 100, 35);
            container.add(btOk);
            container.add(btCancel);
            container.add(tfCookies);
            container.add(label);

            btOk.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    cookies = tfCookies.getText();
                    close();
                }
            });
            btCancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    close();
                }
            });

        }
    }

}
