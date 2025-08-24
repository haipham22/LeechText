package dark.leech.text.ui;

import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMScrollPane;
import dark.leech.text.util.FontUtils;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Long on 10/28/2016.
 */
public class SyntaxDialog extends JMDialog {
    private BasicButton ok;
    private BasicButton cancel;
    private RSyntaxTextArea edit;
    private String text;
    private String title;
    private String type;

    public SyntaxDialog(String title, String text, String type) {
        this.title = title;
        this.text = text;
        this.type = type;
        this.setSize(360, 410);

        onCreate();


    }

    @Override
    protected void onCreate() {
        super.onCreate();
        PanelTitle pnTitle = new PanelTitle();
        ok = new BasicButton();
        cancel = new BasicButton();

        pnTitle.setText(title);
        pnTitle.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        container.add(pnTitle);
        pnTitle.setBounds(0, 0, 360, 45);

        ok.setText("OK");
        container.add(ok);
        ok.setBounds(180, 365, 70, 30);
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clickOk();
            }
        });
        cancel.setText("HỦY");
        container.add(cancel);
        cancel.setBounds(270, 365, 70, 30);
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        edit = new RSyntaxTextArea();
        edit.setText(text);
        edit.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
        edit.setSyntaxEditingStyle(type);
        edit.setLineWrap(true);
        edit.setWrapStyleWord(true);
        edit.setMarginLineEnabled(true);
        edit.setCodeFoldingEnabled(true);
        Font font = FontUtils.codeFont(12f);
        if (text != null)
            if (font.canDisplayUpTo(text) == -1)
                edit.setFont(font);

        JMScrollPane scrollPane = new JMScrollPane();
        scrollPane.setViewportView(edit);
        container.add(scrollPane);
        scrollPane.setBounds(10, 47, 348, 310);

    }


    public String getText() {
        return text;
    }


    private void clickOk() {
        text = edit.getText();
        close();
    }


}
