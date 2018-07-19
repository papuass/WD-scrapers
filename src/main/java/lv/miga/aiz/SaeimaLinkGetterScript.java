package lv.miga.aiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lv.miga.aiz.guice.ScraperModule;
import lv.miga.aiz.model.ImmutableMemberLink;
import lv.miga.aiz.model.MemberLink;
import lv.miga.aiz.utils.TextUtils;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.WbSearchEntitiesResult;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SaeimaLinkGetterScript {

    private static final String URL_SAEIMA_12 = "http://titania.saeima.lv/Personal/Deputati/Saeima12_DepWeb_Public.nsf/depArchList.js?OpenPage&count=3000&lang=LV";
    private static final String LINE_START_PATTERN = "depArchRec";
    private TextUtils textUtils;

    @Inject
    public SaeimaLinkGetterScript(TextUtils textUtils) {
        this.textUtils = textUtils;
    }

    public static void main(final String[] args) {
        String outputFileName = args[0];

        Injector injector = Guice.createInjector(new ScraperModule());
        SaeimaLinkGetterScript script = injector.getInstance(SaeimaLinkGetterScript.class);
        script.run(outputFileName);
    }

    private void run(String outputFileName) {
        List<MemberLink> entries = new ArrayList<>();
        try (InputStream is = new URL(URL_SAEIMA_12).openConnection().getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is));
             Stream<String> lines = reader.lines()) {

            lines.forEach(line -> {
                if (line.startsWith(LINE_START_PATTERN)) {
                    entries.add(parseLine(line));
                }
            });

            Files.write(Paths.get(outputFileName),
                    entries.stream().map(this::formatLine).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String formatLine(MemberLink entry) {
        return String.format("%s,%s %s,http://titania.saeima.lv/Personal/Deputati/Saeima12_DepWeb_Public.nsf//0/%s/?OpenDocument&lang=EN",
                entry.getQid(), entry.getName(), entry.getSurname(), entry.getUrl());
    }

    private MemberLink parseLine(String line) {
        MemberLink entry = null;
        Optional<String> value = textUtils.extractFirstValue(LINE_START_PATTERN + "\\((.*)\\);.*", line);
        if (value.isPresent()) {
            JsonNode node = textUtils.getJsonNode(value.get());
            String name = node.get("name").textValue();
            String surname = node.get("sname").textValue();

            entry = ImmutableMemberLink.builder().name(name)
                    .surname(surname).url(node.get("unid").textValue()).qid(findQidByName(String.format("%s %s", name, surname))).build();
        }

        System.out.println(entry);
        return entry;
    }

    private String findQidByName(String label) {
        ApiConnection connection = ApiConnection.getWikidataApiConnection();
        WikibaseDataFetcher dataFetcher = new WikibaseDataFetcher(connection, Datamodel.SITE_WIKIDATA);
        dataFetcher.getFilter().excludeAllLanguages();
        dataFetcher.getFilter().excludeAllSiteLinks();

        String qId = null;
        try {
            List<WbSearchEntitiesResult> results = dataFetcher.searchEntities(label, "lv");
            if (results.size() == 1) {
                qId = results.get(0).getEntityId();

            } else {
                System.out.printf("More than one entry found with name %s", label);
            }
        } catch (MediaWikiApiErrorException e) {
            e.printStackTrace();
        }

        return qId;
    }
}
