package lv.miga.aiz.config;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class WikidataMappings {

    public static final Map<Integer, String> parliaments;
    public static final Map<String, String> groups;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(12, "Q20557340");
        map.put(11, "Q13098708");
        map.put(10, "Q16347625");
        map.put(9, "Q16350138");
        map.put(8, "Q16350102");
        map.put(7, "Q16350051");
        map.put(6, "Q16350006");
        map.put(5, "Q16349907");

        parliaments = Collections.unmodifiableMap(map);
    }

    static {
        Map<String, String> map = new HashMap<>();
        map.put("For Latvia from the Heart parliamentary group", "Q49638223");
        map.put("National Alliance \"All For Latvia!\" – \"For Fatherland and Freedom/LNNK\" parliamentary group", "Q49637732");
        map.put("National Alliance of All for Latvia! and For Fatherland and Freedom/LNNK parliamentary group", "Q49637732");
        map.put("All for Latvia and For Fatherland and Freedom/LNNK parliamentary group", "Q49637732");
        map.put("For Fatherland and Freedom/LNNK parliamentary group", "Q49637732");
        map.put("Parliamentary group of the National Alliance \"All for Latvia\" and \"For Fatherland and Freedom\"/LNNK", "Q49637732");
        map.put("Concord parliamentary group", "Q49637655");
        map.put("Concord Centre parliamentary group", "Q49637655");
        map.put("Unity parliamentary group", "Q49636278");
        map.put("Union of Greens and Farmers parliamentary group", "Q49636011");
        map.put("Greens and Farmers Union parliamentary group", "Q49636011");
        map.put("Latvian Regional Alliance parliamentary group", "Q49637927");
        map.put("Reform Party parliamentary group", "Q56605854");
        map.put("Zatlers' Reform Party parliamentary group", "Q56605854");
        map.put("For a Good Latvia parliamentary group", "Q56640485");
        map.put("New Era parliamentary group", "Q56649629");
        map.put("People's Party parliamentary group", "Q56649653");
        map.put("Civic Union parliamentary group", "Q56649694");
        map.put("Latvia's First Party / Latvia's Way party parliamentary group", "Q56649746");
        map.put("For Human Rights in a United Latvia parliamentary group", "Q56649772");

        map.put("Unaffiliated members of parliament", "Q49638041");

        groups = Collections.unmodifiableMap(map);
    }

}
