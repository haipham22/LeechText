package dark.leech.text.ui.main;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.PluginManager;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMScrollPane;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FontUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

/**
 * Created by Dark on 2/26/2017.
 */
public class HelpUI extends JMDialog {
    private StringBuilder INFO;
    private StringBuilder support;
    private static final String JVM = System.getProperty("java.vm.name");
    private static final String JRE = System.getProperty("java.version");


    public HelpUI() {
        INFO = new StringBuilder();
        INFO.append("<b>LeechText " + AppUtils.VERSION +  "</b>");
        INFO.append("<br>Build: " + AppUtils.VERSION.replace(".", "/") + " at " + AppUtils.TIME +" <u>© 2017 Darkrai</u>");
        INFO.append("<br>");
        INFO.append("<br>JRE: " + JRE);
        INFO.append("<br>JVM: " + JVM);
        support = new StringBuilder();
        for (PluginEntity pl : PluginManager.getManager().list()) {
            support.append("• " + pl.getSource() + "<br>");
        }
        onCreate();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        PanelTitle pnTitle = new PanelTitle();
        BasicButton btClose = new BasicButton();
        BasicButton btVisit = new BasicButton();
        JLabel lb = new JLabel("Các trang hỗ trợ");
        JTextPane jTInfo = new JTextPane();
        JTextPane jTPage = new JTextPane();
        JMScrollPane sc = new JMScrollPane(jTPage);

        pnTitle.setText("Thông tin");
        pnTitle.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        container.add(pnTitle);
        pnTitle.setBounds(0, 0, 300, 45);


        jTInfo.setFont(FontUtils.TEXT_NORMAL);
        jTInfo.setContentType("text/html");
        jTInfo.setText(INFO.toString());
        jTInfo.setEditable(false);
        jTInfo.setBorder(null);
        container.add(jTInfo);
        jTInfo.setBounds(10, 50, 280, 100);

        lb.setFont(FontUtils.TEXT_NORMAL);
        container.add(lb);
        lb.setBounds(10, 145, 200, 30);
        jTPage.setFont(FontUtils.TEXT_NORMAL);
        jTPage.setContentType("text/html");
        jTPage.setText(support.toString());
        jTPage.setEditable(false);
        jTPage.setBorder(null);
        container.add(sc);
        sc.setBounds(10, 175, 280, 185);
        btClose.setText("ĐÓNG");
        btClose.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        container.add(btClose);
        btClose.setBounds(20, 360, 90, 35);

        btVisit.setText("VISIT");
        btVisit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(new URL("https://www.facebook.com/leechtext/").toURI());
                } catch (Exception xe) {
                }
            }
        });
        container.add(btVisit);
        btVisit.setBounds(200, 360, 90, 35);

        setSize(300, 400);

    }
}
