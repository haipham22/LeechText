package dark.leech.text.ui.material;

import dark.leech.text.listeners.BlurListener;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class SelectBox extends JMPanel {
    private int selectIndex;
    private String[] list;
    private JLabel labelName;
    private BlurListener blurListener;
    private ChangeListener changeListener;


    public SelectBox(String name, String[] list, int selectIndex) {
        setLayout(new BorderLayout());
        this.selectIndex = selectIndex;
        this.list = list;

        JPanel panelChooser = new JPanel(new BorderLayout());
        JLabel labelTitle = new JLabel(name);
        labelTitle.setFont(FontUtils.TEXT_NORMAL);
        add(labelTitle, BorderLayout.WEST);
        panelChooser.setBackground(Color.WHITE);
        panelChooser.setOpaque(false);
        panelChooser.setCursor(new Cursor(Cursor.HAND_CURSOR));
        labelName = new JLabel(list[selectIndex]);
        labelName.setHorizontalAlignment(SwingConstants.RIGHT);
        labelName.setFont(FontUtils.TEXT_NORMAL);
        panelChooser.add(labelName, BorderLayout.WEST);
        panelChooser.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                doClick();
            }
        });

        JLabel label = new JLabel(StringUtils.EXPAND);
        label.setFont(FontUtils.ICON_NORMAL);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panelChooser.add(label, BorderLayout.EAST);
        add(panelChooser, BorderLayout.EAST);
    }

    public void setModel(String[] list, int selectIndex) {
        this.list = list;
        this.selectIndex = selectIndex;
        labelName.setText(list[selectIndex]);
    }

    public void addBlurListener(BlurListener blurListener) {
        this.blurListener = blurListener;
    }

    public void addChangeListener(ChangeListener changeListener) {
        this.changeListener = changeListener;
    }

    private void doClick() {
        final JMDialogChooser dc = new JMDialogChooser(list, selectIndex);
        dc.setBlurListener(blurListener);
        dc.setChangeListener(new ChangeListener() {
            @Override
            public void doChanger() {
                selectIndex = dc.getSelectIndex();
                labelName.setText(list[selectIndex]);
                if (changeListener != null)
                    changeListener.doChanger();
            }
        });
        dc.open();

    }

    public int getSelectIndex() {
        return selectIndex;
    }

    public String getSelectText() {
        return list[selectIndex];
    }
}

class JMDialogChooser extends JMDialog {
    private List<ChooserItem> listItem;
    private int selectIndex;
    private String[] list;
    private MouseAdapter mo = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent arg0) {
            for (int i = 0; i < listItem.size(); i++)
                if (arg0.getSource() == listItem.get(i))
                    selectIndex = i;
            close();
        }


    };


    public JMDialogChooser(String[] list, int selectIndex) {
        this.selectIndex = selectIndex;
        this.list = list;
        onCreate();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        container.setLayout(new GridBagLayout());
        container.setBackground(Color.WHITE);
        listItem = new ArrayList<ChooserItem>();
        JPanel body = new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        for (int i = 0; i < list.length; i++) {
            ChooserItem ch = new ChooserItem(list[i]);
            ch.addMouseListener(mo);
            listItem.add(ch);
            body.add(ch);
        }
        listItem.get(selectIndex).setSelected(true);
        container.add(body);
        pack();
    }

    public int getSelectIndex() {
        return selectIndex;
    }
}

class ChooserItem extends JMPanel {
    private JLabel labelName;
    private JLabel labelSelect;
    private boolean selected;

    public ChooserItem(String name) {
        setLayout(null);
        setOpaque(true);
        setBackground(Color.white);
        labelName = new JLabel(name);
        labelName.setFont(FontUtils.TEXT_NORMAL);
        add(labelName);
        labelName.setBounds(10, 0, 130, 40);
        labelSelect = new JLabel();
        labelSelect.setFont(FontUtils.ICON_NORMAL);
        labelSelect.setHorizontalAlignment(SwingConstants.CENTER);
        add(labelSelect);
        labelSelect.setBounds(150, 0, 40, 40);
        setPreferredSize(new Dimension(200, 40));
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        labelSelect.setText(selected ? StringUtils.CHECK : "");
    }

}
