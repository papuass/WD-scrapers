package lv.miga.aiz.utils;

public class TextUtilsImpl implements TextUtils {

    @Override
    public String doubleQuote(String text) {
        return "\"" + text + "\"" ;
    }
}
