package dark.leech.text.util;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

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

    public static String getUUID(String... bases) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            StringBuilder sb = new StringBuilder();
            for (String base : bases) {
                if (base != null) sb.append(base);
            }
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            UUID uuid = UUID.nameUUIDFromBytes(hash);
            return uuid.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }
}
