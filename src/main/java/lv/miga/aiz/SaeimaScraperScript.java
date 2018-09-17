package lv.miga.aiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lv.miga.aiz.config.WikidataMappings;
import lv.miga.aiz.export.WikibaseAPIExportUtilImpl;
import lv.miga.aiz.guice.ScraperModule;
import lv.miga.aiz.model.ImmutableMemberOfParliament;
import lv.miga.aiz.model.ImmutableParliamentaryGroup;
import lv.miga.aiz.model.MemberOfParliament;
import lv.miga.aiz.model.ParliamentaryGroup;
import lv.miga.aiz.utils.DateUtils;
import lv.miga.aiz.utils.TextUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SaeimaScraperScript {

    static Logger logger = LoggerFactory.getLogger(SaeimaScraperScript.class);

    private DateUtils dateUtils;
    private TextUtils textUtils;

    @Inject
    public SaeimaScraperScript(DateUtils dateUtils, TextUtils textUtils) {
        this.dateUtils = dateUtils;
        this.textUtils = textUtils;
    }

    public static void main(final String[] args) {
        String fileName = args[0];

        Injector injector = Guice.createInjector(new ScraperModule());
        SaeimaScraperScript script = injector.getInstance(SaeimaScraperScript.class);

        try (Stream<String> lines = Files.lines(Paths.get(fileName))) {
            lines.forEach(line -> {
                String[] params = line.split(",");
                MemberOfParliament deputy = script.parseUrl(params[0], params[2]);
                logger.info("Exporting {}", deputy);
                new WikibaseAPIExportUtilImpl().export(deputy);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MemberOfParliament parseUrl(String qid, String url) {
        ImmutableMemberOfParliament.Builder builder = ImmutableMemberOfParliament.builder().qid(parseQidValue(qid)).referenceURL(url);
        try {
            Document doc = Jsoup.connect(url).get();

            builder = builder.parliament(getParliament(doc));

            Elements mandates = doc.select("div.viewHolder");
            if (!mandates.isEmpty()) {
                builder = builder.parliamentaryGroups(getDeputyParliamentaryGroups(mandates.get(0).html()));

                Optional<String> value = textUtils.extractFirstValue(".*drawMand\\((.*)\\);.*", mandates.get(1).html());
                if (value.isPresent()) {
                    JsonNode root = textUtils.getJsonNode(value.get());

                    builder = builder.name(root.get("name").textValue())
                            .surname(root.get("sname").textValue())
                            .fromNote(root.get("mrreason").textValue())
                            .toNote(root.get("mfreason").textValue())
                            .replacesDeputy(parseReplacement(root.get("mrreason").textValue()));
                }
            }
            builder.birthYear(parseBirthYear(doc));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return builder.build();
    }

    private Long parseBirthYear(Document doc) {
        String text = doc.select("table.wholeForm tr td").get(1).select("span").get(1).text();
        return Long.parseLong(text);
    }

    private String parseQidValue(String qid) {
        return "".equals(qid) ? null : qid;
    }

    private String parseReplacement(String value) {
        return textUtils.extractFirstValue("Member of the Saeima replacing the former member of the Saeima (.*)", value).orElse(null);
    }

    private Integer getParliament(Document doc) {
        Elements title = doc.select("title");
        if (!title.isEmpty()) {
            Optional<String> value = textUtils.extractFirstValue(".*The (\\d*)th Saeima.*", title.text());
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
            JsonNode root = textUtils.getJsonNode(value);
            if (root != null) {
                Date dateFrom = dateUtils.parseLatvianDate(root.get("dtF").textValue());
                Date dateTo = dateUtils.parseLatvianDate(root.get("dtT").textValue());
                String groupName = root.get("str").textValue();

                Optional<ParliamentaryGroup> parliamentaryGroup = createOrFindMatchingGroup(groups, dateFrom, dateTo, groupName);
                if (parliamentaryGroup.isPresent()) {
                    ParliamentaryGroup group = parliamentaryGroup.get();
                    dateFrom = dateUtils.min(dateFrom, group.getDateFrom());
                    dateTo = dateTo == null || group.getDateTo() == null ? null : dateUtils.max(dateTo, group.getDateTo());
                    groups.remove(group);
                }
                groups.add(ImmutableParliamentaryGroup.builder().groupName(groupName).dateFrom(dateFrom).dateTo(dateTo).build());
            }
        };
    }


    private Optional<ParliamentaryGroup> createOrFindMatchingGroup(Set<ParliamentaryGroup> groups, Date dateFrom, Date dateTo, String groupName) {
        return groups.stream().filter(matchesExistingGroup(dateFrom, dateTo, groupName)).findFirst();
    }

    private Predicate<ParliamentaryGroup> matchesExistingGroup(Date dateFrom, Date dateTo, String groupName) {
        return group ->
        {
            String existingGroup = WikidataMappings.groups.get(group.getGroupName());
            return existingGroup.equals(WikidataMappings.groups.get(groupName))
                    && (group.getDateTo().equals(dateFrom) || group.getDateFrom().equals(dateTo));
        };
    }

}
