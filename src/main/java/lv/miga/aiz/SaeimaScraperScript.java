package lv.miga.aiz;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lv.miga.aiz.guice.ScraperModule;
import lv.miga.aiz.model.MemberOfParliament;
import lv.miga.aiz.model.ParliamentaryGroup;
import lv.miga.aiz.utils.DateUtils;
import lv.miga.aiz.utils.ExportUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SaeimaScraperScript {

    private DateUtils dateUtils;

    @Inject
    public SaeimaScraperScript(DateUtils dateUtils) {
        this.dateUtils = dateUtils;
    }

    public static void main(final String[] args) {
        String url = args[0];
        String qid = args.length > 1 ? args[1] : null;

        Injector injector = Guice.createInjector(new ScraperModule());
        SaeimaScraperScript script = injector.getInstance(SaeimaScraperScript.class);

        MemberOfParliament deputy = script.parseUrl(qid, url);
        System.out.println(deputy);
        System.out.println();
        injector.getInstance(ExportUtil.class).export(deputy);
    }

    private MemberOfParliament parseUrl(String qid, String url) {
        MemberOfParliament deputy = new MemberOfParliament();
        deputy.setQid(qid);
        deputy.setReferenceURL(url);
        try {
            Document doc = Jsoup.connect(url).get();

            Elements mandates = doc.select("div.viewHolder");
            if (!mandates.isEmpty()) {
                deputy.setParliamentaryGroups(getDeputyParliamentaryGroups(mandates.get(0).html()));

                Pattern p = Pattern.compile(".*drawMand\\((.*)\\);.*");
                Matcher m = p.matcher(mandates.get(1).html());
                if (m.find()) {
                    JsonNode root = getJsonNode(m.group(1));

                    deputy.setName(root.get("name").textValue());
                    deputy.setSurname(root.get("sname").textValue());
                    deputy.setFromNote(root.get("mrreason").textValue());
                    deputy.setToNote(root.get("mfreason").textValue());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return deputy;
    }

    private List<ParliamentaryGroup> getDeputyParliamentaryGroups(String code) throws IOException {
        Set<ParliamentaryGroup> groups = new HashSet<>();

        Pattern p = Pattern.compile(".*drawWN\\((.*strLvlTp:\"2\".*)\\);.*");
        Matcher m = p.matcher(code);
        while (m.find()) {
            JsonNode root = getJsonNode(m.group(1));

            Date dateFrom = dateUtils.parseLatvianDate(root.get("dtF").textValue());
            Date dateTo = dateUtils.parseLatvianDate(root.get("dtT").textValue());
            String groupName = root.get("str").textValue();

            ParliamentaryGroup parliamentaryGroup = createOrFindMatchingGroup(groups, dateFrom, dateTo, groupName);
            parliamentaryGroup.setDateFrom(dateUtils.min(dateFrom, parliamentaryGroup.getDateFrom()));
            parliamentaryGroup.setDateTo(dateTo == null || parliamentaryGroup.getDateTo() == null ? null : dateUtils.max(dateTo, parliamentaryGroup.getDateTo()));
            groups.add(parliamentaryGroup);
        }

        return groups.stream().sorted(Comparator.comparing(ParliamentaryGroup::getDateFrom)).collect(Collectors.toList());
    }

    private ParliamentaryGroup createOrFindMatchingGroup(Set<ParliamentaryGroup> groups, Date dateFrom, Date dateTo, String groupName) {
        return groups.stream().filter(matchesExistingGroup(dateFrom, dateTo, groupName)).findFirst().orElseGet(() -> {
                    ParliamentaryGroup group = new ParliamentaryGroup();
                    group.setGroupName(groupName);
                    group.setDateFrom(dateFrom);
                    group.setDateTo(dateTo);
                    return group;
                }
        );
    }

    private Predicate<ParliamentaryGroup> matchesExistingGroup(Date dateFrom, Date dateTo, String groupName) {
        return group -> group.getGroupName().equals(groupName) && (group.getDateTo().equals(dateFrom) || group.getDateFrom().equals(dateTo));
    }

    private JsonNode getJsonNode(String jsoupText) throws IOException {
        String json = Parser.unescapeEntities(jsoupText, true);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        return mapper.readTree(json);
    }

}
