package lv.miga.aiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lv.miga.aiz.config.SaeimaWebsiteMappings;
import lv.miga.aiz.guice.ScraperModule;
import lv.miga.aiz.model.ImmutableMemberLink;
import lv.miga.aiz.model.MemberLink;
import lv.miga.aiz.utils.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.WbSearchEntitiesResult;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SaeimaLinkGetterScript {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String LINE_START_PATTERN = "depArchRec";
    private TextUtils textUtils;

    @Inject
    public SaeimaLinkGetterScript(TextUtils textUtils) {
        this.textUtils = textUtils;
    }

    public static void main(final String[] args) {
        int parliamentaryTerm = Integer.parseInt(args[0]);
        String outputFileName = args[1];

        Injector injector = Guice.createInjector(new ScraperModule());
        SaeimaLinkGetterScript script = injector.getInstance(SaeimaLinkGetterScript.class);
        script.run(parliamentaryTerm, outputFileName);
    }

    private void run(int parliamentaryTerm, String outputFileName) {
        List<MemberLink> entries = new ArrayList<>();
        try (InputStream is = new URL(SaeimaWebsiteMappings.parliaments.get(parliamentaryTerm)).openConnection().getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is));
             Stream<String> lines = reader.lines()) {

            lines.forEach(line -> {
                if (line.startsWith(LINE_START_PATTERN)) {
                    entries.add(parseLine(line, parliamentaryTerm));
                }
            });

            Files.write(Paths.get(outputFileName),
                    entries.stream().map(this::formatLine).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatLine(MemberLink entry) {
        return String.format(SaeimaWebsiteMappings.profileUrls.get(entry.getParliamentaryTerm()),
                entry.getQid(), entry.getName(), entry.getSurname(), entry.getUrl());
    }

    private MemberLink parseLine(String line, int parliamentaryTerm) {
        MemberLink entry = null;
        Optional<String> value = textUtils.extractFirstValue(LINE_START_PATTERN + "\\((.*)\\);.*", line);
        if (value.isPresent()) {
            JsonNode node = textUtils.getJsonNode(value.get());
            String name = node.get("name").textValue();
            String surname = node.get("sname").textValue();

            entry = ImmutableMemberLink.builder()
                    .name(name)
                    .surname(surname)
                    .parliamentaryTerm(parliamentaryTerm)
                    .url(node.get("unid").textValue())
                    .qid(findQidByName(String.format("%s %s", name, surname)))
                    .build();
        }

        return entry;
    }

    private String findQidByName(String label) {
        ApiConnection connection = ApiConnection.getWikidataApiConnection();
        WikibaseDataFetcher dataFetcher = new WikibaseDataFetcher(connection, Datamodel.SITE_WIKIDATA);
        dataFetcher.getFilter().excludeAllLanguages();
        dataFetcher.getFilter().excludeAllSiteLinks();

        String qId = null;
        try {
            List<WbSearchEntitiesResult> results = dataFetcher.searchEntities(label, "lv")
                    .stream().filter(getExtraFilterPredicate(label)).collect(Collectors.toList());
            if (results.size() == 1) {
                qId = results.get(0).getEntityId();
            } else if (results.size() > 1) {
                qId = "MULTIPLE_RESULTS_FOUND";
                logger.info("More than one entry found using name {}, {}", label, getWikidataSerchLink(label));
            } else {
                qId = "NOT_FOUND";
                logger.info("Nothing found using name {}, {}", label, getWikidataSerchLink(label));
            }
        } catch (MediaWikiApiErrorException e) {
            e.printStackTrace();
        }

        return qId;
    }

    private String getWikidataSerchLink(String label) {
        try {
            return "https://www.wikidata.org/w/index.php?search=" + URLEncoder.encode(label, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Predicate<WbSearchEntitiesResult> getExtraFilterPredicate(String label) {
        return result -> {
            String matchType = result.getMatch().getType();
            String matchText = result.getMatch().getText();
            return ("label".equals(matchType) || "alias".equals(matchType)) && matchText.equals(label);
        };
    }
}
