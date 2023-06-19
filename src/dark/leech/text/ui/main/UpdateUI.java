package dark.leech.text.ui.main;

import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.Http;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by Dark on 2/25/2017.
 */
public class UpdateUI extends JMDialog {
    private static final String URL = "https://dl.dropboxusercontent.com/s/c701wrrsbhl63l2/update.json?dl=1";
    private JSONObject obj;


    public static void checkUpdate() {
        final int VERSION = Integer.parseInt(AppUtils.VERSION.replace(".", ""));
        final int TIME = Integer.parseInt(AppUtils.TIME.replace(":", ""));
        try {
            String update = new String(Http.connect(URL)
                    .execute()
                    .bodyAsBytes(), StandardCharsets.UTF_8);
            JSONObject obj = new JSONObject(update);
            int version = Integer.parseInt(obj.getString("version").replace(".", ""));
            int time = Integer.parseInt(obj.getString("time").replace(":", ""));

            if (version > VERSION || (version == VERSION && time > TIME))
                new UpdateUI(obj).open();


        } catch (Exception e) {
        }
    }

    public UpdateUI(JSONObject obj) {
        this.obj = obj;
        onCreate();
    }


    @Override
    protected void onCreate() {
        super.onCreate();
        PanelTitle panelTitle = new PanelTitle();
        BasicButton btUpdate = new BasicButton();
        BasicButton btCancel = new BasicButton();
        JLabel lbInfo = new JLabel("Có bản update mới! v" + obj.getString("version") + " " + obj.getString("time"));

        lbInfo.setFont(FontUtils.TEXT_NORMAL);
        container.add(lbInfo);
        lbInfo.setBounds(25, 60, 250, 25);
        panelTitle.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        panelTitle.setText("Cập nhật");
        container.add(panelTitle);
        panelTitle.setBounds(0, 0, 310, 45);

        btUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                update();
            }
        });
        btUpdate.setText("CẬP NHẬT");
        container.add(btUpdate);
        btUpdate.setBounds(45, 100, 110, 35);

        btCancel.addActionListener(new ActionListener() {
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
        String cmd = FileUtils.validate("java -jar tools/update.jar ");
        cmd += "\"" + obj.getString("url") + "\" \"" + FileUtils.validate(AppUtils.curDir + "/tools/LeechText.jar") + "\"";
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
        }
        System.exit(0);
    }

}



