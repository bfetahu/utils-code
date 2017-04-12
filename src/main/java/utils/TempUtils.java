package utils;

import org.joda.time.DateTime;

import java.util.regex.Pattern;

/**
 * Created by besnik on 2/26/17.
 */
public class TempUtils {
    private static Pattern[] temp_regex;

    private static String temp_regex_str = "[0-9]{2}\\s+(January|February|March|April|May|June|July|August|September|October|November|December)\\s+[0-9]{2,4}\n" +
            "[0-9]-[0-9]{2}-[0-9]{2,4}\n" +
            "[0-9]\\.[0-9]{2}\\.[0-9]{2,4}\n" +
            "[0-9]{4}";

    /**
     * Load the temporal regular expressions.
     */
    public static Pattern[] loadTempRegex() {
        if (temp_regex == null) {
            String[] regex = temp_regex_str.split("\n");
            temp_regex = new Pattern[regex.length];
            for (int i = 0; i < regex.length; i++) {
                temp_regex[i] = Pattern.compile(regex[i]);
            }
            return temp_regex;
        }
        return temp_regex;
    }

    /**
     * First we try to parse the date into the format YYYY-MM-dd by applying a set of date format patterns.
     * Then we return the corresponding date object.
     * @param temp_expr
     * @return
     */
    public static DateTime parseDate(String temp_expr) {
        String parsed_date = Utils.parseDate(temp_expr);
        return new DateTime(parsed_date);
    }
}
