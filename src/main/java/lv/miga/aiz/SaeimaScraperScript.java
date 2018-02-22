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
import lv.miga.aiz.utils.TextUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SaeimaScraperScript {

    private DateUtils dateUtils;
    private TextUtils textUtils;

    @Inject
    public SaeimaScraperScript(DateUtils dateUtils, TextUtils textUtils) {
        this.dateUtils = dateUtils;
        this.textUtils = textUtils;
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

            deputy.setParliament(getParliament(doc));

            Elements mandates = doc.select("div.viewHolder");
            if (!mandates.isEmpty()) {
                deputy.setParliamentaryGroups(getDeputyParliamentaryGroups(mandates.get(0).html()));

                Optional<String> value = textUtils.extractValue(".*drawMand\\((.*)\\);.*", mandates.get(1).html());
                if (value.isPresent()) {
                    JsonNode root = getJsonNode(value.get());

                    deputy.setName(root.get("name").textValue());
                    deputy.setSurname(root.get("sname").textValue());
                    deputy.setFromNote(root.get("mrreason").textValue());
                    deputy.setToNote(root.get("mfreason").textValue());
                    deputy.setReplacesDeputy(parseReplacement(root.get("mrreason").textValue()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return deputy;
    }

    private String parseReplacement(String value) {
        return textUtils.extractValue("Member of the Saeima replacing the former member of the Saeima (.*)", value).orElse(null);
    }

    private Integer getParliament(Document doc) {
        Elements title = doc.select("title");
        if (!title.isEmpty()) {
            Optional<String> value = textUtils.extractValue(".*The (\\d*)th Saeima.*", title.text());
            if (value.isPresent()) {
                return Integer.parseInt(value.get());
            }
        }
        return null;
    }

    private List<ParliamentaryGroup> getDeputyParliamentaryGroups(String code) {
        Set<ParliamentaryGroup> groups = new HashSet<>();
        textUtils.extractValues(".*drawWN\\((.*strLvlTp:\"2\".*)\\);.*", code).forEach(processParliamentaryGroupEntry(groups));
        return groups.stream().sorted(Comparator.comparing(ParliamentaryGroup::getDateFrom)).collect(Collectors.toList());
    }

    private Consumer<String> processParliamentaryGroupEntry(Set<ParliamentaryGroup> groups) {
        return value -> {
            JsonNode root = getJsonNode(value);
            if (root != null) {
                Date dateFrom = dateUtils.parseLatvianDate(root.get("dtF").textValue());
                Date dateTo = dateUtils.parseLatvianDate(root.get("dtT").textValue());
                String groupName = root.get("str").textValue();

                ParliamentaryGroup parliamentaryGroup = createOrFindMatchingGroup(groups, dateFrom, dateTo, groupName);
                parliamentaryGroup.setDateFrom(dateUtils.min(dateFrom, parliamentaryGroup.getDateFrom()));
                parliamentaryGroup.setDateTo(dateTo == null || parliamentaryGroup.getDateTo() == null ? null : dateUtils.max(dateTo, parliamentaryGroup.getDateTo()));
                groups.add(parliamentaryGroup);
            }
        };
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

    private JsonNode getJsonNode(String jsoupText) {
        String json = Parser.unescapeEntities(jsoupText, true);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        try {
            return mapper.readTree(json);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
