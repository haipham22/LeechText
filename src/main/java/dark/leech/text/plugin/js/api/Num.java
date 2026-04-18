package dark.leech.text.plugin.js.api;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import dark.leech.text.action.Log;

/**
 * Number utilities API for JavaScript plugins using Rhino. Provides number parsing, formatting, and
 * conversion.
 */
public class Num extends JsApiWrapper {

    /**
     * Create Num API with execution context for proper object creation.
     *
     * @param context The Rhino Context for script execution
     * @param scope The Rhino Scriptable scope
     */
    public Num(Context context, Scriptable scope) {
        super(context, scope);
    }

    /** Create Num API without context (legacy compatibility). */
    public Num() {
        super();
    }

    /** Parse string to integer. Usage: num.parseInt("123") => 123 */
    public int parseInt(String text) {
        return parseInt(text, 10);
    }

    /** Parse string to integer with radix. Usage: num.parseInt("FF", 16) => 255 */
    public int parseInt(String text, int radix) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(text.trim(), radix);
        } catch (NumberFormatException e) {
            Log.add("Failed to parse integer: " + text);
            return 0;
        }
    }

    /** Parse string to double. Usage: num.parseDouble("3.14") => 3.14 */
    public double parseDouble(String text) {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            Log.add("Failed to parse double: " + text);
            return 0.0;
        }
    }

    /** Format number with decimal places. Usage: num.format(3.14159, 2) => "3.14" */
    public String format(double number, int decimals) {
        try {
            String format = "%." + decimals + "f";
            return String.format(format, number);
        } catch (Exception e) {
            Log.add("Failed to format number: " + number);
            return String.valueOf(number);
        }
    }

    /**
     * Format number with thousands separator. Usage: num.formatThousands(1234567) => "1,234,567"
     */
    public String formatThousands(long number) {
        return String.format("%,d", number);
    }

    public String formatThousands(double number) {
        return String.format("%,f", number);
    }

    /** Round number to decimal places. Usage: num.round(3.14159, 2) => 3.14 */
    public double round(double number, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(number * factor) / factor;
    }

    /**
     * Clamp number between min and max. Usage: num.clamp(5, 0, 10) => 5 Usage: num.clamp(-5, 0, 10)
     * => 0
     */
    public double clamp(double number, double min, double max) {
        return Math.max(min, Math.min(max, number));
    }

    /**
     * Check if string is a valid number. Usage: num.isNumeric("123") => true Usage:
     * num.isNumeric("abc") => false
     */
    public boolean isNumeric(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(text.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** Convert bytes to human-readable size. Usage: num.bytesToHuman(1536) => "1.5 KB" */
    public String bytesToHuman(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char unit = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), unit);
    }

    /** Convert percentage string to decimal. Usage: num.percentToDecimal("50%") => 0.5 */
    public double percentToDecimal(String percent) {
        if (percent == null) {
            return 0.0;
        }
        String cleaned = percent.trim().replace("%", "");
        try {
            return Double.parseDouble(cleaned) / 100.0;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /** Generate random number between min and max (exclusive). Usage: num.random(0, 10) => 0-9 */
    public int random(int min, int max) {
        return min + (int) (Math.random() * (max - min));
    }

    /**
     * Generate random number between min and max (inclusive). Usage: num.randomInclusive(1, 10) =>
     * 1-10
     */
    public int randomInclusive(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    /** Convert Arabic numerals to Chinese numerals. Usage: num.toChineseNumerals(123) => "一百二十三" */
    public String toChineseNumerals(int number) {
        if (number == 0) {
            return "零";
        }

        String[] digits = {"", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
        String[] units = {"", "十", "百", "千", "万"};

        StringBuilder result = new StringBuilder();
        String numStr = String.valueOf(number);
        int length = numStr.length();

        for (int i = 0; i < length; i++) {
            int digit = numStr.charAt(i) - '0';
            int position = length - i - 1;

            if (digit != 0) {
                result.append(digits[digit]);
                if (position < units.length) {
                    result.append(units[position]);
                }
            } else if (i < length - 1) {
                result.append("零");
            }
        }

        return result.toString();
    }

    /**
     * Convert Chinese numerals to Arabic numerals. Usage: num.fromChineseNumerals("一百二十三") => 123
     */
    public int fromChineseNumerals(String chinese) {
        if (chinese == null || chinese.isEmpty()) {
            return 0;
        }

        Map<String, Integer> digitMap = new HashMap<>();
        digitMap.put("零", 0);
        digitMap.put("一", 1);
        digitMap.put("二", 2);
        digitMap.put("三", 3);
        digitMap.put("四", 4);
        digitMap.put("五", 5);
        digitMap.put("六", 6);
        digitMap.put("七", 7);
        digitMap.put("八", 8);
        digitMap.put("九", 9);

        Map<String, Integer> unitMap = new HashMap<>();
        unitMap.put("十", 10);
        unitMap.put("百", 100);
        unitMap.put("千", 1000);
        unitMap.put("万", 10000);

        int result = 0;
        int current = 0;

        for (char c : chinese.toCharArray()) {
            String ch = String.valueOf(c);
            if (digitMap.containsKey(ch)) {
                current = digitMap.get(ch);
            } else if (unitMap.containsKey(ch)) {
                int unit = unitMap.get(ch);
                if (current == 0) {
                    current = 1;
                }
                result += current * unit;
                current = 0;
            }
        }

        return result + current;
    }
}
