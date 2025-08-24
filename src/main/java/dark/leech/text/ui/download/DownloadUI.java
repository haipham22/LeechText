package dark.leech.text.ui.download;

import dark.leech.text.action.History;
import dark.leech.text.listeners.AddListener;
import dark.leech.text.listeners.RemoveListener;
import dark.leech.text.models.Properties;
import dark.leech.text.ui.material.JMPanel;
import dark.leech.text.ui.material.JMScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.io.File;

/**
 * Created by Dark on 1/14/2017.
 */
public class DownloadUI extends JPanel implements AddListener, RemoveListener, DropTargetListener {
    private JMPanel download;
    private JMScrollPane scrollPane;

    public DownloadUI() {
        onCreate();
    }

    private void onCreate() {
        setLayout(null);
        download = new JMPanel(new GridBagLayout());
        download.setBackground(Color.white);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.weighty = 1;
        scrollPane = new JMScrollPane(download);

        JPanel demo = new JPanel();
        demo.setBackground(new Color(0,0,0,0));
        download.add(demo, gbc);

        add(scrollPane);
        scrollPane.setBounds(0, 55, 390, 475);
        new DropTarget(this, this);
    }

    public void actionAdd(Properties properties, boolean imp) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        DownloadLabel dl = new DownloadLabel();

        if (imp) dl.importDownload(properties);
        else dl.addDownload(properties);

        dl.addRemoveListener(this);
        download.add(dl, gbc, 0);
        download.updateUI();
    }

    @Override
    public void removeComponent(Component comp) {
        download.remove(comp);
        download.updateUI();
    }

    @Override
    public void drop(DropTargetDropEvent event) {
        event.acceptDrop(DnDConstants.ACTION_COPY);
        Transferable transferable = event.getTransferable();
        DataFlavor[] flavors = transferable.getTransferDataFlavors();
        for (DataFlavor flavor : flavors) {
            try {
                if (flavor.isFlavorJavaFileListType()) {
                    java.util.List<File> files = (java.util.List) transferable.getTransferData(flavor);
                    for (File file : files) {
                        String path = file.getPath();
                        AddDialog dialog = new AddDialog(History.getHistory().load(path));
                        dialog.setAddListener(this);
                        dialog.open();
                    }
                }
            } catch (Exception e) {
            }
        }
        event.dropComplete(true);
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {

    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    @Override
    public void dragExit(DropTargetEvent dte) {

    }

    @Override
    public void addDownload(Properties properties, boolean imp) {
        actionAdd(properties, imp);
    }


}
