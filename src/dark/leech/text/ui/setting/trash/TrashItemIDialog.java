package dark.leech.text.ui.setting.trash;

import dark.leech.text.models.Trash;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMTextField;
import dark.leech.text.util.FontUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Dark on 2/13/2017.
 */
class TrashItemIDialog extends JMDialog {
    private JMTextField textSrc;
    private JMTextField textTo;
    private JMTextField textTip;
    private BasicButton ok;
    private BasicButton cancel;
    private Trash trash;

    public TrashItemIDialog() {
        this(new Trash());
    }

    public TrashItemIDialog(Trash trash) {
        this.trash = trash;
        onCreate();
        setTrash(trash);
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        JLabel labelSrc = new JLabel();
        JLabel labelTo = new JLabel();
        JLabel labelTip = new JLabel();
        textSrc = new JMTextField();
        textTo = new JMTextField();
        textTip = new JMTextField();
        ok = new BasicButton();
        cancel = new BasicButton();

        labelSrc.setText("Tìm");
        labelSrc.setFont(FontUtils.TEXT_NORMAL);
        container.add(labelSrc);
        container.add(textSrc);
        textSrc.setBounds(10, 35, 280, 37);
        labelSrc.setBounds(10, 10, 280, 25);
        labelTo.setText("Thay thế bởi");
        labelTo.setFont(FontUtils.TEXT_NORMAL);
        container.add(labelTo);
        container.add(textTo);
        textTo.setBounds(10, 100, 280, 37);
        labelTo.setBounds(10, 75, 280, 25);
        labelTip.setText("Mô tả");
        labelTip.setFont(FontUtils.TEXT_NORMAL);
        container.add(labelTip);
        container.add(textTip);
        textTip.setBounds(10, 160, 280, 37);
        labelTip.setBounds(10, 135, 280, 25);
        ok.setText("XONG");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                check();
            }
        });
        container.add(ok);
        ok.setBounds(105, 210, 90, 30);
        cancel.setText("HỦY");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        container.add(cancel);
        cancel.setBounds(200, 210, 90, 30);
        this.setSize(300, 250);
    }

    public Trash getTrash() {
        trash.setSrc(textSrc.getText());
        trash.setTo((textTo.getText() == null ? "" : textTo.getText()));
        trash.setTip(textTip.getText());
        trash.setReplace(trash.isReplace());
        return trash;
    }

    public void setTrash(Trash trash) {
        this.trash = trash;
        textSrc.setText(trash.getSrc());
        textTo.setText(trash.getTo());
        textTip.setText(trash.getTip());
    }

    private void check() {
        if (textSrc.getText().length() == 0) {
            textSrc.addError("Nội dung không được để trống!");
            return;
        }
        close();
    }

}
