package dark.leech.text.ui.main;

import java.awt.*;

import javax.swing.*;

import dark.leech.text.action.Log;
import dark.leech.text.ui.Animation;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.SettingUtils;

public class App {

    private static MainUI mainFrame;

    public static void main(String[] args) {
        // Load application icon (handles macOS dock icon automatically)
        AppUtils.loadApplicationIcon();

        new Thread(
                        () -> {
                            try {
                                UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                            } catch (Exception ex) {
                            }
                            AppUtils.doLoad();
                            FileUtils.init();
                            SettingUtils.doLoad();
                            Log.add("Home directory: " + AppUtils.curDir);
                            mainFrame = new MainUI();
                            Animation.fadeIn(mainFrame);
                            mainFrame.setVisible(true);
                            AppUtils.LOCATION =
                                    new Point(
                                            mainFrame.getLocation().x,
                                            mainFrame.getLocation().y + 20);
                        })
                .start();
    }

    public static MainUI getMain() {
        return mainFrame;
    }
}
