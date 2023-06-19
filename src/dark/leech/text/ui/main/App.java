package dark.leech.text.ui.main;

import dark.leech.text.repository.RepositoryManager;
import dark.leech.text.ui.Animation;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.SettingUtils;

import javax.swing.*;
import java.awt.*;

public class App {

    private static MainUI mainFrame;
    private static int openCount;
    private static int closeCount;

    public static void main(String[] args) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                } catch (Exception ex) {
                }
                AppUtils.doLoad();
                FileUtils.init();
                SettingUtils.doLoad();
                RepositoryManager.getManager().doLoad();
                mainFrame = new MainUI();
                Animation.fadeIn(mainFrame);
                mainFrame.setVisible(true);
                AppUtils.LOCATION = new Point(mainFrame.getLocation().x, mainFrame.getLocation().y + 20);
            }
        }).start();
    }

    public static MainUI getMain() {
        return mainFrame;
    }


}
