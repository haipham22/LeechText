package dark.leech.text.ui.setting.trash;

import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.listeners.RemoveListener;
import dark.leech.text.models.Trash;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dark on 2/13/2017.
 */
class TrashUI extends JMDialog implements RemoveListener {
    public int numTrash = 0;
    private List<Trash> trash;
    private BasicButton add;
    private BasicButton ok;
    private BasicButton cancel;
    private JPanel body;
    private GridBagConstraints gbc;
    private boolean done;

    public TrashUI(List<Trash> trash) {
        done = false;
        numTrash = 0;
        this.trash = trash;
        onCreate();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                load();
            }
        });

    }

    @Override
    protected void onCreate() {
        super.onCreate();
        add = new BasicButton();
        ok = new BasicButton();
        cancel = new BasicButton();
        body = new JPanel(new GridBagLayout());
        body.setBackground(Color.white);
        PanelTitle pnTitle = new PanelTitle();


        pnTitle.setText("Lọc rác");
        pnTitle.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        pnTitle.setBounds(0, 0, 330, 45);
        container.add(pnTitle);
        body.setBackground(Color.white);
        GridBagConstraints gi = new GridBagConstraints();
        gi.gridwidth = GridBagConstraints.REMAINDER;
        gi.weightx = 1;
        gi.weighty = 1;
        JMScrollPane scrollPane = new JMScrollPane(body);

        JPanel demo = new JPanel();
        demo.setBackground(Color.WHITE);
        body.add(demo, gi);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        container.add(scrollPane);
        scrollPane.setBounds(0, 45, 327, 270);
        //
        add.setText("THÊM");
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addItem();
            }
        });
        container.add(add);
        add.setBounds(10, 320, 100, 30);

        ok.setText("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                done = true;
                close();
            }
        });
        container.add(ok);
        ok.setBounds(170, 320, 70, 30);

        cancel.setText("HỦY");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        container.add(cancel);
        cancel.setBounds(250, 320, 70, 30);
        gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        setSize(330, 370);
    }

    public void load() {
        for (int i = 0; i < trash.size(); i++)
            addItem(trash.get(i));
    }

    public List<Trash> getTrash() {
        trash = new ArrayList<Trash>();
        for (int i = 0; i < body.getComponentCount() - 1; i++)
            trash.add(((TrashItem) body.getComponent(i)).getTrash());
        return trash;
    }

    private void addItem() {
        final TrashItemIDialog trashItemDialog = new TrashItemIDialog();
        trashItemDialog.setBlurListener(this);
        trashItemDialog.setChangeListener(new ChangeListener() {
            @Override
            public void doChanger() {
                addItem(trashItemDialog.getTrash());
            }
        });
        trashItemDialog.open();


    }

    private void addItem(Trash trash) {
        TrashItem tr = new TrashItem(trash);
        tr.addBlurListener(this);
        tr.addRemoveListener(this);
        body.add(tr, gbc, numTrash);
        validate();
        repaint();
        numTrash++;
    }
    @Override
    public void removeComponent(Component comp) {
        body.remove(comp);
        body.updateUI();
        numTrash--;
    }

}
