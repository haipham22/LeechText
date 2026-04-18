package dark.leech.text.ui;

/** Created by Long on 9/3/2016. */
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import lombok.Getter;

import dark.leech.text.models.Chapter;
import dark.leech.text.models.Pager;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.JMDialog;
import dark.leech.text.ui.material.JMScrollPane;
import dark.leech.text.ui.material.JMTable;

public class ListDialog extends JMDialog {

    private JMScrollPane scPn;
    private JMTable tbList;
    private BasicButton btCancel;
    private BasicButton btOk;
    private BasicButton btSelect;
    private PanelTitle pnTitle;
    private List<Chapter> chapList;
    private List<Pager> pageList;
    @Getter private String parseList;
    private boolean selected = true;
    private final boolean forum;

    public ListDialog(List<Pager> pageList, String parseList, boolean forum) {
        this.pageList = pageList;
        this.parseList = parseList;
        this.forum = forum;
        onCreate();
    }

    public ListDialog(List<Chapter> chapList, String parseList) {
        this.parseList = parseList;
        this.chapList = chapList;
        forum = false;
        onCreate();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        scPn = new JMScrollPane();
        btCancel = new BasicButton();
        btOk = new BasicButton();
        btSelect = new BasicButton();
        pnTitle = new PanelTitle();

        // ======== scPn ========
        Object[] columnNames = {"", "Tên chương"};
        Object[][] data = null;
        if (forum) {
            // Handle null or empty pageList
            if (pageList == null || pageList.isEmpty()) {
                data = new Object[0][2];
            } else {
                data = new Object[pageList.size()][2];
                for (int i = 0; i < pageList.size(); i++) {
                    data[i][0] = false;
                    data[i][1] = pageList.get(i).getName();
                }
            }
        } else {
            // Handle null or empty chapList
            if (chapList == null || chapList.isEmpty()) {
                data = new Object[0][2];
            } else {
                data = new Object[chapList.size()][2];
                for (int i = 0; i < chapList.size(); i++) {
                    data[i][0] = false;
                    data[i][1] = chapList.get(i).getChapName();
                }
            }
        }
        tbList = new JMTable();
        doUpdateData(parseList, data);
        tbList.setModel(
                new DefaultTableModel(data, columnNames) {
                    final Class<?>[] columnTypes = new Class<?>[] {Boolean.class, String.class};

                    @Override
                    public Class<?> getColumnClass(int columnIndex) {
                        return columnTypes[columnIndex];
                    }
                });
        doUpdateData(parseList, data);
        TableColumnModel cm = tbList.getColumnModel();
        cm.getColumn(0).setMaxWidth(30);
        tbList.setTableHeader(null);
        scPn.setViewportView(tbList);
        scPn.getVerticalScrollBar().setUnitIncrement(20);
        container.add(scPn);
        scPn.setBounds(0, 50, 348, 420);

        // ---- btCancel ----
        btCancel.setText("HỦY");
        btCancel.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        close();
                    }
                });
        container.add(btCancel);
        btCancel.setBounds(275, 475, 65, 30);

        // ---- btOk ----
        btOk.setText("OK");
        container.add(btOk);
        btOk.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        okClick();
                    }
                });
        btOk.setBounds(210, 475, 60, 30);

        // ---- btSelect ----
        btSelect.setText("BỎ CHỌN");
        container.add(btSelect);
        btSelect.addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        doSelect();
                    }
                });
        btSelect.setBounds(5, 475, 100, 30);

        // ---- lbTitle ----
        pnTitle.setText("Danh sách chương");
        pnTitle.addCloseListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        close();
                    }
                });
        container.add(pnTitle);
        pnTitle.setBounds(0, 0, 350, 45);
        this.setSize(350, 520);
    }

    private void okClick() {
        ArrayList<Integer> c = new ArrayList<Integer>();
        DefaultTableModel tb = (DefaultTableModel) tbList.getModel();
        for (int i = 0; i < tbList.getRowCount(); i++) {
            if (tbList.getCellEditor() != null) tbList.getCellEditor().stopCellEditing();
            // Null check for chapList before accessing
            if (!forum && chapList != null && i < chapList.size()) {
                chapList.get(i).setChapName((String) tbList.getValueAt(i, 1));
            }
            if ((boolean) tbList.getValueAt(i, 0)) c.add(i + 1);
        }
        if (c.size() == 0) {

            return;
        }
        doParseList(c);
        close();
    }

    private void doSelect() {

        selected = !selected;
        btSelect.setText(selected ? "BỎ CHỌN" : "CHỌN");
        if (tbList.getSelectedRowCount() != 0) {
            int[] sl = tbList.getSelectedRows();

            for (int i = 0; i < sl.length; i++) tbList.setValueAt(selected, sl[i], 0);
        } else {
            for (int i = 0; i < tbList.getRowCount(); i++) tbList.setValueAt(selected, i, 0);
        }
    }

    public void doUpdateData(String parseList, Object[][] data) {
        parseList = parseList.replaceAll("\\s+", "");
        if (parseList.isEmpty()) {
            return;
        }
        String[] c = parseList.split(",");
        for (String s : c) {
            try {
                String[] cc = s.split("-");
                int start = Integer.parseInt(cc[0]);
                int end = cc.length > 1 ? Integer.parseInt(cc[cc.length - 1]) : start;

                for (int j = start; j <= end; j++) {
                    // Bounds check to prevent ArrayIndexOutOfBoundsException
                    if (j > 0 && j <= data.length) {
                        data[j - 1][0] = true;
                    }
                }
            } catch (NumberFormatException e) {
                // Skip invalid range format
                continue;
            }
        }
        //        if (parseList.equals("1-" + i2s(chapList.size())))
        //            return;

    }

    // Loc chuong rut gon
    public void doParseList(ArrayList<Integer> intMang) {
        parseList = "";
        for (int i = 0; i < intMang.size() - 1; i++)
            if (intMang.get(i) + 1 == intMang.get(i + 1)) parseList += i2s(intMang.get(i)) + "-";
            else parseList += i2s(intMang.get(i)) + ",";
        parseList += i2s(intMang.get(intMang.size() - 1));
        parseList = parseList.replaceAll("(\\d+-).*?(\\d+(,|$))", "$1$2");
    }

    private String i2s(int i) {
        return Integer.toString(i);
    }
}
