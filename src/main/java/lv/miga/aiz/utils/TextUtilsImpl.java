package lv.miga.aiz.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.parser.Parser;

import java.io.IOException;
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

    @Override
    public JsonNode getJsonNode(String text) {
        String json = Parser.unescapeEntities(text, true);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        try {
            return mapper.readTree(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
