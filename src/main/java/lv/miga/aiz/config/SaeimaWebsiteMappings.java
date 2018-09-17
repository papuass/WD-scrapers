package lv.miga.aiz.config;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class SaeimaWebsiteMappings {

    public static final Map<Integer, String> parliaments;
    public static final Map<Integer, String> profileUrls;

    static {
        Map<Integer, String> map = new HashMap<>();
        Arrays.asList(10, 11, 12).forEach(nr -> map.put(nr, String.format("http://titania.saeima.lv/Personal/Deputati/Saeima%s_DepWeb_Public.nsf/depArchList.js?OpenPage&count=3000&lang=LV", nr)));
        map.put(9, "http://titania.saeima.lv/Personal/Deputati/Saeima_DepWeb_Public.nsf/depArchList.js?OpenPage&count=3000&lang=LV");
        Arrays.asList(0, 5, 6, 7, 8).forEach(nr -> map.put(nr, "http://titania.saeima.lv/Personal/Deputati/Saeima_DepWeb_Archive.nsf/depArchList.js?OpenPage&count=3000&lang=LV"));
        parliaments = Collections.unmodifiableMap(map);
    }

    static {
        Map<Integer, String> map = new HashMap<>();
        Arrays.asList(10, 11, 12).forEach(nr -> map.put(nr, "%s,%s %s,http://titania.saeima.lv/Personal/Deputati/Saeima" + nr + "_DepWeb_Public.nsf/0/%s/?OpenDocument&lang=EN"));
        map.put(9, "%s,%s %s,http://titania.saeima.lv/Personal/Deputati/Saeima_DepWeb_Public.nsf/0/%s/?OpenDocument&lang=EN");

        profileUrls = Collections.unmodifiableMap(map);
    }

}
