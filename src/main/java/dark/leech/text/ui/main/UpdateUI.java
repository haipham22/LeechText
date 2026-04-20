package dark.leech.text.ui.main;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.charset.StandardCharsets;

import javax.swing.*;

import org.json.JSONObject;

import dark.leech.text.action.Log;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.Http;

/** Created by Dark on 2/25/2017. */
public class UpdateUI extends JMDialog {
    private static final String URL =
            "https://raw.githubusercontent.com/haipham22/LeechText/main/gradle.properties";
    private final JSONObject obj;

    public UpdateUI(JSONObject obj) {
        this.obj = obj;
        onCreate();
    }

    public static void checkUpdate() {
        final int VERSION = Integer.parseInt(AppUtils.VERSION.replace(".", ""));
        try {
            String properties =
                    new String(Http.connect(URL).execute().bodyAsBytes(), StandardCharsets.UTF_8);

            // Parse gradle.properties format: "app.version=1.1.0"
            String latestVersion = "0";
            String versionString = "0.0.0";
            for (String line : properties.split("\n")) {
                if (line.startsWith("app.version=")) {
                    versionString = line.substring("app.version=".length());
                    latestVersion = versionString.replace(".", "");
                    break;
                }
            }

            int version = Integer.parseInt(latestVersion);

            if (version > VERSION) {
                // Create a minimal JSONObject for the update dialog
                JSONObject updateInfo = new JSONObject();
                updateInfo.put("tag_name", "v" + versionString);
                updateInfo.put("name", "Latest Release");
                updateInfo.put(
                        "html_url", "https://github.com/haipham22/LeechText/releases/latest");
                new UpdateUI(updateInfo).open();
            }

        } catch (Exception e) {
            Log.add(e);
        }
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        PanelTitle panelTitle = new PanelTitle();
        BasicButton btUpdate = new BasicButton();
        BasicButton btCancel = new BasicButton();
        JLabel lbInfo =
                new JLabel(
                        "Có bản update mới! "
                                + obj.getString("tag_name")
                                + " - "
                                + obj.getString("name"));

        lbInfo.setFont(FontUtils.TEXT_NORMAL);
        container.add(lbInfo);
        lbInfo.setBounds(25, 60, 250, 25);
        panelTitle.addCloseListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        close();
                    }
                });
        panelTitle.setText("Cập nhật");
        container.add(panelTitle);
        panelTitle.setBounds(0, 0, 310, 45);

        btUpdate.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        update();
                    }
                });
        btUpdate.setText("CẬP NHẬT");
        container.add(btUpdate);
        btUpdate.setBounds(45, 100, 110, 35);

        btCancel.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        close();
                    }
                });
        btCancel.setText("HỦY");
        container.add(btCancel);
        btCancel.setBounds(165, 100, 110, 35);

        setSize(310, 160);
        super.onCreate();
    }

    private void update() {
        try {
            // Get the browser download URL from GitHub release
            String htmlUrl = obj.getString("html_url"); // GitHub release page

            // Open browser to download the update
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(htmlUrl));

        } catch (Exception e) {
            Log.add("Error opening update URL: " + e.getMessage());
        }
        close();
    }
}
