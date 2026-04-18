package dark.leech.text.ui.main;

import java.awt.*;
import java.net.URI;

import javax.swing.*;

import dark.leech.text.enities.PluginEntity;
import dark.leech.text.plugin.PluginManager;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMScrollPane;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FontUtils;

/**
 * Help/About dialog displaying application information and support links.
 *
 * <p>Shows app version, build info, JVM details, and plugin sources. Provides button to visit
 * Facebook support page.
 *
 * @author Dark
 * @since 2017-02-26
 */
public class HelpUI extends JMDialog {
    private static final String DIALOG_TITLE = "Thông tin";
    private static final String SUPPORT_LABEL = "Các trang hỗ trợ";
    private static final String CLOSE_BUTTON_TEXT = "ĐÓNG";
    private static final String VISIT_BUTTON_TEXT = "VISIT";
    private static final String WEBSITE = "https://github.com/haipham22/LeechText/";

    private static final int DIALOG_WIDTH = 300;
    private static final int DIALOG_HEIGHT = 400;

    public HelpUI() {
        onCreate();
    }

    @Override
    protected void onCreate() {
        super.onCreate();

        setupTitlePanel();
        setupInfoPanel();
        setupSupportPanel();
        setupButtons();

        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
    }

    private void setupTitlePanel() {
        PanelTitle titlePanel = new PanelTitle();
        titlePanel.setText(DIALOG_TITLE);
        titlePanel.addCloseListener(e -> close());
        container.add(titlePanel);
        titlePanel.setBounds(0, 0, DIALOG_WIDTH, 45);
    }

    private void setupInfoPanel() {
        JTextPane infoText = createHtmlTextPane(buildInfoHtml());
        container.add(infoText);
        infoText.setBounds(10, 50, DIALOG_WIDTH - 20, 100);
    }

    private void setupSupportPanel() {
        JLabel supportLabel = new JLabel(SUPPORT_LABEL);
        supportLabel.setFont(FontUtils.TEXT_NORMAL);
        container.add(supportLabel);
        supportLabel.setBounds(10, 145, 200, 30);

        JTextPane supportText = createHtmlTextPane(buildSupportHtml());
        JMScrollPane scrollPane = new JMScrollPane(supportText);
        container.add(scrollPane);
        scrollPane.setBounds(10, 175, DIALOG_WIDTH - 20, 185);
    }

    private void setupButtons() {
        BasicButton closeButton = new BasicButton();
        closeButton.setText(CLOSE_BUTTON_TEXT);
        closeButton.addActionListener(e -> close());
        container.add(closeButton);
        closeButton.setBounds(20, 360, 90, 35);

        BasicButton visitButton = new BasicButton();
        visitButton.setText(VISIT_BUTTON_TEXT);
        visitButton.addActionListener(e -> openFacebookPage());
        container.add(visitButton);
        visitButton.setBounds(DIALOG_WIDTH - 110, 360, 90, 35);
    }

    private JTextPane createHtmlTextPane(String html) {
        JTextPane textPane = new JTextPane();
        textPane.setFont(FontUtils.TEXT_NORMAL);
        textPane.setContentType("text/html");
        textPane.setText(html);
        textPane.setEditable(false);
        textPane.setBorder(null);
        return textPane;
    }

    private String buildInfoHtml() {
        String jre = System.getProperty("java.version");
        String jvm = System.getProperty("java.vm.name");
        String buildVersion = AppUtils.VERSION.replace(".", "/");

        // Using String.format() - compatible with Java 17+ (STR templates require Java 21+ preview)
        return String.format(
                "<b>LeechText %s</b>"
                        + "<br>Build: %s at %s <u>© %s Darkrai + Hải Phạm</u>"
                        + "<br><br>JRE: %s"
                        + "<br>JVM: %s",
                AppUtils.VERSION, buildVersion, AppUtils.TIME, AppUtils.COPYRIGHT, jre, jvm);
    }

    private String buildSupportHtml() {
        StringBuilder html = new StringBuilder();
        for (PluginEntity plugin : PluginManager.getManager().list()) {
            html.append("• ").append(plugin.getSource()).append("<br>");
        }
        return html.toString();
    }

    private void openFacebookPage() {
        try {
            Desktop.getDesktop().browse(new URI(WEBSITE));
        } catch (Exception e) {
            // Silently handle browser open failure
        }
    }
}
