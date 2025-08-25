package dark.leech.text.util;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

public class TextUtils {
    public static boolean isEmpty(String text) {
        return text == null || text.length() == 0;
    }

    public static String getClipboard() {
        try {
            Transferable transferable =
                    Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (transferable != null
                    && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String result = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                if (result.toLowerCase().startsWith("http")) return result;
            }
        } catch (Exception e) {
        }
        return "";
    }
}
