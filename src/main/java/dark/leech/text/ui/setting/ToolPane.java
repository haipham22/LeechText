package dark.leech.text.ui.setting;

import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.main.App;
import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.util.AppUtils;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Code by Darkrai on 8/23/2016.
 */
public class ToolPane extends JMPanel {
    private JLabel lbPath;
    private boolean selectDirectory = false;


    public ToolPane(String name,final String path,final int type) {
        JLabel lbName = new JLabel();
        lbPath = new JLabel();

        lbName.setText(name);
        lbName.setFont(FontUtils.TEXT_BOLD);
        lbName.setBounds(25, 5, 290, 30);
        add(lbName);

        lbPath.setText(path);
        lbPath.setFont(FontUtils.TEXT_THIN);
        lbPath.setForeground(Color.GRAY);
        lbPath.setBounds(25, 30, 290, 30);
        add(lbPath);

        CircleButton btEdit = new CircleButton(StringUtils.EDIT, 23f);
        btEdit.setForeground(ColorUtils.THEME_COLOR);
        btEdit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FileDialog f = new FileDialog(App.getMain(), "Chọn đường dẫn", type);
                if (type == FileDialog.SAVE)
                    f.setFile("LeechText");
                f.setModal(true);
                if (getPath().equals("Chưa đặt"))
                    f.setDirectory(System.getProperty("user.dir"));
                else
                    f.setDirectory(getPath());
                f.setVisible(true);
                if (f.getDirectory() != null)
                    setPath(f.getDirectory() + ((selectDirectory) ? "" : f.getFile()));
            }
        });
        add(btEdit);
        btEdit.setBounds(335, 15, 30, 30);
        setBackground(Color.white);
        setPreferredSize(new Dimension(370, 60));
        setLayout(null);
    }

    public void setSelectDirectory(boolean selectDirectory) {
        this.selectDirectory = selectDirectory;
    }

    public String getPath() {
        return lbPath.getText();
    }

    public void setPath(String path) {
        if (path.length() != 0)
            if (path.lastIndexOf(AppUtils.SEPARATOR) == path.length() - 1)
                path = path.substring(0, path.length() - 1);
        lbPath.setText(path);
    }

}

