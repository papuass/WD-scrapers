package lv.miga.aiz.utils;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Optional;

public interface TextUtils {

    String doubleQuote(String text);

    /**
     * @param pattern - expects pattern with one matching group
     * @param text - text where to look for pattern
     * @return - returns matched text
     */
    Optional<String> extractFirstValue(String pattern, String text);

    /**
     * @param pattern expects pattern with one matching group
     * @param text - text where to look for pattern
     * @return - returns list of matched texts
     */
    List<String> extractValues(String pattern, String text);

    JsonNode getJsonNode(String text);
}
