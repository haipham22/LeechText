package dark.leech.text.ui.main.export.config;

import dark.leech.text.listeners.ChangeListener;
import dark.leech.text.ui.PanelTitle;
import dark.leech.text.ui.button.BasicButton;
import dark.leech.text.ui.material.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Created by Dark on 3/8/2017.
 */
class FindAndReplace extends JMDialog {
    private JMTable table;
    private JMTextField tfFind;
    private JMTextField tfReplace;
    private JMCheckBox cbRegex;
    private JMCheckBox cbCase;
    private BasicButton btReplace;
    private JMProgressBar progressBar;

    public FindAndReplace(JMTable table) {
        this.table = table;
        setSize(290, 270);
        onCreate();
    }

    @Override
    protected void onCreate() {
        super.onCreate();
        setBorderColor(Color.LIGHT_GRAY);
        PanelTitle pnTitle = new PanelTitle();
        JLabel lbFind = new JLabel();
        tfFind = new JMTextField();
        JLabel lbReplace = new JLabel();
        tfReplace = new JMTextField();
        cbRegex = new JMCheckBox("Regex", true);
        cbCase = new JMCheckBox("Match case", true);
        btReplace = new BasicButton();
        progressBar = new JMProgressBar();

        pnTitle.setText("Thay thế");
        pnTitle.addCloseListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        container.add(pnTitle);
        pnTitle.setBounds(0, 0, 290, 45);
        container.add(tfFind);
        tfFind.setBounds(25, 70, 250, 35);
        container.add(tfReplace);
        tfReplace.setBounds(25, 130, 250, 35);

        //---- lbFind ----
        lbFind.setText("Tìm");
        container.add(lbFind);
        lbFind.setBounds(10, 45, 65, 25);

        //---- lbReplace ----
        lbReplace.setText("Thay thế");
        container.add(lbReplace);
        lbReplace.setBounds(10, 105, 70, 25);

        //---- cbRegex ----
        container.add(cbRegex);
        cbRegex.setChangeListener(new ChangeListener() {
            @Override
            public void doChanger() {
                cbCase.setVisible(cbRegex.isChecked());
            }
        });
        cbRegex.setBounds(25, 170, 90, 30);

        //---- cbCase ----
        container.add(cbCase);
        cbCase.setVisible(cbRegex.isChecked());
        cbCase.setBounds(25, 200, 100, 30);

        //---- btReplace ----
        btReplace.setText("Thay thế");
        btReplace.setBackground(Color.LIGHT_GRAY);
        btReplace.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btReplace.setEnabled(false);
                        replace();
                        btReplace.setEnabled(true);
                    }
                });
            }
        });
        container.add(btReplace);
        btReplace.setBounds(150, 170, 125, 60);

        //---- lbResult ----
        container.add(progressBar);
        progressBar.setVisible(false);
        progressBar.setBounds(30, 230, 250, 19);

    }


    private void replace() {
        String tFind = tfFind.getText();
        String tReplace = tfReplace.getText();
        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        progressBar.setVisible(true);
        if (table.getCellEditor() != null) table.getCellEditor().stopCellEditing();
        for (int i = 0; i < table.getRowCount(); i++) {
            for (int j = 1; j < 3; j++) {
                String text = (String) tableModel.getValueAt(i, j);
                if (text != null)
                    if (text.length() != 0)
                        if (cbRegex.isChecked())
                            text = text.replaceAll(cbCase.isChecked() ? "(?i)" : "" + tFind, tReplace);
                        else
                            text = text.replace(tFind, tReplace);
                tableModel.setValueAt(text, i, j);
                tableModel.fireTableCellUpdated(i, j);
            }
            progressBar.setPercent(i * 100 / table.getRowCount());
        }
        progressBar.setVisible(false);
    }
}
