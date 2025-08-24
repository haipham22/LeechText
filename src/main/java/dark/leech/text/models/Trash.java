package dark.leech.text.models;

/**
 * Code by Darkrai on 8/21/2016.
 */
public class Trash {
    private String src;
    private String to;
    private String tip;
    private boolean replace;

    public Trash() {
        this("", "", "", true);
    }

    public Trash(String src, String to, String tip, boolean replace) {
        this.src = src;
        this.to = to;
        this.tip = tip;
        this.replace = replace;
    }

    public boolean isReplace() {
        return replace;
    }

    public void setReplace(boolean replace) {
        this.replace = replace;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getTo() {
        if (to == null) to = "";
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }
}