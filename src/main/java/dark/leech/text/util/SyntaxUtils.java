package dark.leech.text.util;

/**
 * Created by Dark on 2/11/2017.
 */
public class SyntaxUtils {
    private SyntaxUtils() {
    }

    private static final String[] dau = new String[]{"̀â", "ấ", "ậ", "ẩ", "ẫ", "à", "á", "ạ", "ả", "ã", "â", "ằ", "ắ", "ặ", "ẳ", "ẵ", "ă", "ề", "ế", "ệ", "ể", "ễ", "è", "é", "ẹ", "ẻ", "ẽ", "ê", "ì", "í", "ị", "ỉ", "ĩ", "ồ", "ố", "ộ", "ổ", "ỗ", "ò", "ó", "ọ", "ỏ", "õ", "ô", "ờ", "ớ", "ợ", "ở", "ỡ", "ơ", "ù", "ú", "ụ", "ủ", "ũ", "ừ", "ứ", "ự", "ử", "ữ", "ư", "ỳ", "ý", "ỵ", "ỷ", "ỹ", "đ", "Ầ", "Ấ", "Ậ", "Ẩ", "Ẫ", "À", "Á", "Ạ", "Ả", "Ã", "Â", "Ằ", "Ắ", "Ặ", "Ẳ", "Ẵ", "Ă", "Ề", "Ế", "Ệ", "Ể", "Ễ", "È", "É", "Ẹ", "Ẻ", "Ẽ", "Ê", "Ì", "Í", "Ị", "Ỉ", "Ĩ", "Ồ", "Ố", "Ộ", "Ổ", "Ỗ", "Ò", "Ó", "Ọ", "Ỏ", "Õ", "Ô", "Ờ", "Ớ", "Ợ", "Ở", "Ỡ", "Ơ", "Ù", "Ú", "Ụ", "Ủ", "Ũ", "Ừ", "Ứ", "Ự", "Ử", "Ữ", "Ư", "Ỳ", "Ý", "Ỵ", "Ỷ", "Ỹ", "Đ"};
    private static final String[] src = new String[]{"ầ", "ấ", "ậ", "ẩ", "ẫ", "à", "á", "ạ", "ả", "ã", "â", "ằ", "ắ", "ặ", "ẳ", "ẵ", "ă", "ề", "ế", "ệ", "ể", "ễ", "è", "é", "ẹ", "ẻ", "ẽ", "ê", "ì", "í", "ị", "ỉ", "ĩ", "ồ", "ố", "ộ", "ổ", "ỗ", "ò", "ó", "ọ", "ỏ", "õ", "ô", "ờ", "ớ", "ợ", "ở", "ỡ", "ơ", "ù", "ú", "ụ", "ủ", "ũ", "ừ", "ứ", "ự", "ử", "ữ", "ư", "ỳ", "ý", "ỵ", "ỷ", "ỹ", "đ", "Ầ", "Ấ", "Ậ", "Ẩ", "Ẫ", "À", "Á", "Ạ", "Ả", "Ã", "Â", "Ằ", "Ắ", "Ặ", "Ẳ", "Ẵ", "Ă", "Ề", "Ế", "Ệ", "Ể", "Ễ", "È", "É", "Ẹ", "Ẻ", "Ẽ", "Ê", "Ì", "Í", "Ị", "Ỉ", "Ĩ", "Ồ", "Ố", "Ộ", "Ổ", "Ỗ", "Ò", "Ó", "Ọ", "Ỏ", "Õ", "Ô", "Ờ", "Ớ", "Ợ", "Ở", "Ỡ", "Ơ", "Ù", "Ú", "Ụ", "Ủ", "Ũ", "Ừ", "Ứ", "Ự", "Ử", "Ữ", "Ư", "Ỳ", "Ý", "Ỵ", "Ỷ", "Ỹ", "Đ"};
    private static final String[] xoa = new String[]{"a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "e", "e", "e", "e", "e", "e", "e", "e", "e", "e", "e", "i", "i", "i", "i", "i", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "o", "u", "u", "u", "u", "u", "u", "u", "u", "u", "u", "u", "y", "y", "y", "y", "y", "d", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "A", "E", "E", "E", "E", "E", "E", "E", "E", "E", "E", "E", "I", "I", "I", "I", "I", "O", "O", "O", "O", "O", "O", "O", "O", "O", "O", "O", "O", "O", "O", "O", "O", "O", "U", "U", "U", "U", "U", "U", "U", "U", "U", "U", "U", "Y", "Y", "Y", "Y", "Y", "D"};
    public static String[] REPLACE_FROM;
    public static String[] REPLACE_TO;
    public static String CHAP_NAME;
    public static String PART_NAME;

    // convert string
    public static String covertString(String source) {
        for (int i = 0; i < src.length; i++)
            source = source.replace(dau[i], src[i]);
        return source;
    }

    // xóa dấu
    public static String xoaDau(String source) {
        source = covertString(source);
        for (int i = 0; i < src.length; i++)
            source = source.replace(src[i], xoa[i]);
        return source;
    }

    //fix name
    public static String fixName(String source) {
        if (source == null) return null;
        source = covertString(source)
                .trim()
                .replaceAll(":+", ":")
                .replaceAll("\\.+", ".")
                .replaceAll(" 0{1,2}(\\d+)", " $1");

        return source;
    }

    public static String Optimize(String src) {
        if (src == null) return src;
        if (REPLACE_FROM != null)
            for (int i = 0; i < REPLACE_FROM.length; i++)
                src = src.replaceAll(REPLACE_FROM[i], REPLACE_TO[i]);

        return src;
    }

}
