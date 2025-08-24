package dark.leech.text.ui.main.export.config;

import dark.leech.text.action.Config;
import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.listeners.TableListener;
import dark.leech.text.models.Chapter;
import dark.leech.text.models.Properties;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.button.CircleButton;
import dark.leech.text.ui.material.*;
import dark.leech.text.util.ColorUtils;
import dark.leech.text.util.FileUtils;
import dark.leech.text.util.FontUtils;
import dark.leech.text.util.StringUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dark on 3/8/2017.
 */
class ListUI extends JMDialog implements TableListener, ActionListener, KeyListener {
    private List<Chapter> chapList;
    private JMTable tableList;
    private DefaultTableModel tableModel;
    private JMPopupMenu popupMenu;
    private JMPopupMenu popupAction;
    private String name;
    private String[] nameButton = new String[]{"Auto Fix", "Tải ảnh", "Tải Lại", "Optimize"};
    private BasicButton bt3;
    private JMProgressBar progressBar;
    private int action;
    private String path;

    private BasicButton btOk;
    private BasicButton btCancel;
    private JMMenuItem menuEdit;
    private JMMenuItem menuGoto;
    private JMMenuItem menuCopy;
    private JMMenuItem menuDelete;
    private JMMenuItem menuDeletes;
    private JMMenuItem menuMerge;
    private CircleButton btSearch;
    private ArrayList<Integer> idList;

    private Properties properties;

    public ListUI(List<Chapter> chapList, String name) {
        this.chapList = chapList;
        this.name = name;
        setSize(380, 430);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onCreate();
            }
        });
    }

    public ListUI(List<Chapter> chapList, String name, String path) {
        this.chapList = chapList;
        this.name = name;
        this.path = path;
        setSize(380, 430);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                onCreate();
            }
        });
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        idList = new ArrayList<>();
        tableList = new JMTable(chapList);
        for (int i = 0; i < chapList.size(); i++)
            idList.add(i);
        PanelTitle pnTitle = new PanelTitle();
        JMScrollPane scrollPane1 = new JMScrollPane();
        tableList = new JMTable(chapList);
        tableModel = (DefaultTableModel) tableList.getModel();
        btCancel = new BasicButton();
        btOk = new BasicButton();
        bt3 = new BasicButton();
        progressBar = new JMProgressBar();
        popupMenu = new JMPopupMenu();
        popupAction = new JMPopupMenu();
        menuEdit = new JMMenuItem("Sửa");
        menuGoto = new JMMenuItem("Đi tới");
        menuCopy = new JMMenuItem("Copy Link");
        menuDelete = new JMMenuItem("Xóa");
        menuDeletes = new JMMenuItem("Xóa");
        menuMerge = new JMMenuItem("Gộp");
        btSearch = new CircleButton(StringUtils.SEARCH, 25f);

        //======== this ========
        pnTitle.setText(name);
        pnTitle.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        pnTitle.add(btSearch);
        btSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FindAndReplace findAndReplace = new FindAndReplace(tableList);
                findAndReplace.setBlurListener(ListUI.this);
                findAndReplace.open();
            }
        });
        btSearch.setBounds(290, 5, 35, 35);
        container.add(pnTitle);
        pnTitle.setBounds(0, 0, 380, 45);


        scrollPane1.setViewportView(tableList);
        scrollPane1.getVerticalScrollBar().setUnitIncrement(20);
        container.add(scrollPane1);
        scrollPane1.setBounds(0, 45, 380, 335);

        //---- btCancel ----
        btCancel.setText("HỦY");
        btCancel.addActionListener(this);
        container.add(btCancel);
        btCancel.setBounds(300, 390, 75, 30);

        //---- btOk ----
        btOk.setText("OK");
        btOk.addActionListener(this);
        container.add(btOk);
        btOk.setBounds(200, 390, 75, 30);

        //---- bt3 ----
        bt3.setText(nameButton[action]);
        bt3.addActionListener(this);
        container.add(bt3);
        bt3.setBounds(10, 390, 160, 30);
        progressBar.setPercent(0);
        container.add(progressBar);
        progressBar.setBounds(10, 390, 160, 30);
        progressBar.setVisible(false);

        menuMerge.addActionListener(this);
        menuDelete.addActionListener(this);
        menuDeletes.addActionListener(this);
        popupAction.add(menuDeletes);
        popupAction.add(menuMerge);

        menuGoto.addActionListener(this);
        menuCopy.addActionListener(this);
        menuEdit.addActionListener(this);
        popupMenu.add(menuEdit);
        popupMenu.add(menuDelete);
        popupMenu.add(menuCopy);
        popupMenu.add(menuGoto);
        popupMenu.setBorder(new LineBorder(ColorUtils.THEME_COLOR.brighter()));
        popupAction.setBorder(new LineBorder(ColorUtils.THEME_COLOR.brighter()));

        tableList.addMouseListener(new MouseAdapter() {
            private void showIfPopupTrigger(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    if (tableList.getSelectedRowCount() == 1)
                        popupMenu.show(mouseEvent.getComponent(),
                                mouseEvent.getX(), mouseEvent.getY());
                    else if (tableList.getSelectedRowCount() > 1) {
                        popupAction.show(mouseEvent.getComponent(),
                                mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }

            public void mousePressed(MouseEvent mouseEvent) {
                showIfPopupTrigger(mouseEvent);
            }

            public void mouseReleased(MouseEvent mouseEvent) {
                showIfPopupTrigger(mouseEvent);
            }
        });
        tableList.addKeyListener(this);

    }

    public void setAction(int action) {
        this.action = action;
    }

    private void doAction() {
        bt3.setVisible(false);
        progressBar.setVisible(true);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (action) {
                    case ConfigUI.NAME:
                        fixName();
                        break;
                    case ConfigUI.IMG:
                        downImg();
                        break;
                    case ConfigUI.ERROR:
                        fixError();
                        break;
                    case ConfigUI.OPTIMIZE:
                        Optimize();
                        break;
                }
            }
        });
    }

    private void doEdit() {
        final int row = tableList.getSelectedRow();
        final Chapter chapter = chapList.get(idList.get(row));
        ListUI.Edit edit = new ListUI.Edit(chapter);
        edit.setBlurListener(this);
        edit.setChangeListener(new ChangeListener() {
            @Override
            public void doChanger() {
                tableList.setValueAt(chapter.getPartName(), row, 1);
                tableList.setValueAt(chapter.getChapName(), row, 2);
            }
        });
        edit.open();

    }

    private void doGoto() {
        int row = tableList.getSelectedRow();
        Chapter chapter = chapList.get(idList.get(row));
        try {
            Desktop.getDesktop().browse(new URL(chapter.getUrl()).toURI());
        } catch (Exception xe) {
        }
    }

    private void doCopy() {
        int row = tableList.getSelectedRow();
        Chapter chapter = chapList.get(idList.get(row));
        StringSelection stringSelection = new StringSelection(chapter.getUrl());
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        clpbrd.setContents(stringSelection, null);
    }

    private void doDelete() {
        int[] rows = tableList.getSelectedRows();
        for (int i = rows.length - 1; i >= 0; i--) {
            tableModel.removeRow(rows[i]);
            idList.remove(rows[i]);
        }
    }

    private void doMerge() {
        int[] rows = tableList.getSelectedRows();
        File file = new File(FileUtils.validate(path + "/raw/" + chapList.get(idList.get(rows[0])).getId() + ".txt"));
        for (int i = 1; i < rows.length; i++) {
            FileUtils.add2file(new File(FileUtils.validate(path + "/raw/" + chapList.get(idList.get(rows[i])).getId() + ".txt")), file);
            tableModel.removeRow(rows[rows.length - i]);
            idList.remove(rows[rows.length - i]);
        }
    }

    private void doSave() {
        int vt = idList.size() - 1;
        if (tableList.getCellEditor() != null) tableList.getCellEditor().stopCellEditing();
        for (int i = chapList.size() - 1; i >= 0; i--) {
            if (i == idList.get(vt)) {
                chapList.get(i).setPartName((String) tableList.getValueAt(vt, 1));
                chapList.get(i).setChapName((String) tableList.getValueAt(vt, 2));
                vt--;
            } else chapList.remove(i);
        }
        close();
    }

    private void fixName() {
        Config config = new Config(chapList);
        config.addTableListener(this);
        config.autoFixName();
    }

    private void downImg() {
        Config config = new Config(chapList);
        config.addTableListener(this);
        config.setPath(path);
        config.downloadImg();
    }

    private void fixError() {
        Config config = new Config(chapList);
        config.setBlurListener(this);
        config.addTableListener(this);
        config.downloadChap(properties);
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    private void Optimize() {
        Config config = new Config(chapList);
        config.addTableListener(this);
        config.Optimize();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == menuCopy)
            doCopy();
        if (e.getSource() == menuEdit)
            doEdit();
        if (e.getSource() == menuGoto)
            doGoto();
        if (e.getSource() == menuDeletes || e.getSource() == menuDelete)
            doDelete();
        if (e.getSource() == menuMerge)
            doMerge();
        if (e.getSource() == bt3)
            doAction();
        if (e.getSource() == btOk)
            doSave();
        if (e.getSource() == btCancel)
            close();
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if ((e.getKeyCode() == KeyEvent.VK_H) && ((e.getModifiers() & KeyEvent.CTRL_MASK) != 0)) {
            FindAndReplace findAndReplace = new FindAndReplace(tableList);
            findAndReplace.setBlurListener(ListUI.this);
            findAndReplace.open();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    private class Edit extends JMDialog {
        private PanelTitle pnTitle;
        private JMTextField tfPart;
        private JMTextField tfChap;
        private JMScrollPane scrollPane1;
        private JTextArea taText;
        private JLabel lbPart;
        private JLabel lbChap;
        private BasicButton btOk;
        private BasicButton btCancel;
        private Chapter chapter;

        public Edit(Chapter chapter) {
            this.chapter = chapter;
            onCreate();
        }

        @Override
        protected void onCreate() {
            super.onCreate();
            pnTitle = new PanelTitle();
            tfPart = new JMTextField();
            tfChap = new JMTextField();
            scrollPane1 = new JMScrollPane();
            taText = new JTextArea();
            lbPart = new JLabel();
            lbChap = new JLabel();
            btOk = new BasicButton();
            btCancel = new BasicButton();

            //======== this ========
            pnTitle.setText(chapter.getChapName());
            pnTitle.addCloseListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    close();
                }
            });
            container.add(pnTitle);
            pnTitle.setBounds(0, 0, 310, 45);
            container.add(tfPart);
            tfPart.setText(chapter.getPartName());
            tfPart.setBounds(80, 55, 220, 30);
            container.add(tfChap);
            tfChap.setBounds(80, 95, 220, 30);
            tfChap.setText(chapter.getChapName());


            scrollPane1.setViewportView(taText);
            taText.setLineWrap(true);
            taText.setWrapStyleWord(true);
            Font font = FontUtils.codeFont(12f);
            String text = FileUtils.file2string(path + "/raw/" + chapter.getId() + ".txt");
            if (text != null)
                if (font.canDisplayUpTo(text) == -1)
                    taText.setFont(font);
            taText.setText(text);
            taText.setForeground(ColorUtils.THEME_COLOR);
            container.add(scrollPane1);
            scrollPane1.setBounds(10, 135, 290, 220);

            //---- lbPart ----
            lbPart.setText("Quyển");
            container.add(lbPart);
            lbPart.setBounds(5, 55, 70, 30);

            //---- lbChap ----
            lbChap.setText("Chương");
            container.add(lbChap);
            lbChap.setBounds(5, 95, 70, 30);
            btOk.setText("OK");
            container.add(btOk);
            btOk.setBounds(125, 360, 75, 30);
            btOk.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    doClick();
                }
            });
            btCancel.setText("HỦY");
            container.add(btCancel);
            btCancel.setBounds(225, 360, 75, 30);
            btCancel.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    close();
                }
            });
            setSize(310, 400);
        }

        private void doClick() {
            chapter.setPartName(tfPart.getText());
            chapter.setChapName(tfChap.getText());
            FileUtils.string2file(taText.getText(), path + "/raw/" + chapter.getId() + ".txt");
            close();
        }

    }

    @Override
    public void updateData(int row, Chapter chapter) {
        if (tableList.getCellEditor() != null) tableList.getCellEditor().stopCellEditing();
        tableModel.setValueAt("Ok", row, 0);
        tableModel.setValueAt(chapter.getPartName(), row, 1);
        tableModel.setValueAt(chapter.getChapName(), row, 2);
        tableModel.fireTableCellUpdated(row, 0);
        tableModel.fireTableCellUpdated(row, 1);
        tableModel.fireTableCellUpdated(row, 2);
        progressBar.setPercent((row + 1) * 100 / chapList.size());
        if (row + 1 == chapList.size()) progressBar.setVisible(false);
    }
}
