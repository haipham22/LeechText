package dark.leech.text.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Code by Darkrai on 8/21/2016. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Trash {
    private String src = "";
    private String to = "";
    private String tip = "";
    private boolean replace = true;

    public String getTo() {
        if (to == null) to = "";
        return to;
    }
}
