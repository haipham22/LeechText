package dark.leech.text.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Dark on 2/11/2017.
 */
public class RegexUtils {
    private RegexUtils() {
    }

    public static synchronized String find(String src, String regex, int group) {
        try {
            Pattern r = Pattern.compile(regex, Pattern.MULTILINE);
            Matcher m = r.matcher(src);
            if (m.find())
                return m.group(group);
            else
                return null;
        } catch (Exception e) {
            return null;
        }
    }

}
