package lv.miga.aiz.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtilsImpl implements TextUtils {

    @Override
    public String doubleQuote(String text) {
        return String.format("\"%s\"", text);
    }

    @Override
    public Optional<String> extractFirstValue(String pattern, String text) {
        return extractValues(pattern, text).stream().findFirst();
    }

    @Override
    public List<String> extractValues(String pattern, String text) {
        List<String> values = new ArrayList<>();
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        while (m.find()) {
            values.add(m.group(1));
        }
        return values;
    }
}
