package lv.miga.aiz.utils;

import lv.miga.aiz.model.MemberOfParliament;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.StatementBuilder;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.util.WebResourceFetcherImpl;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.LoginFailedException;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.util.ArrayList;
import java.util.List;

public class WikibaseAPIExportUtilImpl implements ExportUtil {

    @Override
    public void export(MemberOfParliament member) {

        WebResourceFetcherImpl.setUserAgent("Saeima bot 0.0.1; Wikidata Toolkit; Java");
        ApiConnection connection = ApiConnection.getWikidataApiConnection();
        try {
            connection.login("my username", "my password");
        } catch (LoginFailedException e) {
            e.printStackTrace();
        }

        WikibaseDataFetcher dataFetcher = new WikibaseDataFetcher(connection, Datamodel.SITE_WIKIDATA);
        dataFetcher.getFilter().excludeAllLanguages();
        dataFetcher.getFilter().excludeAllSiteLinks();

        WikibaseDataEditor wbde = new WikibaseDataEditor(connection, Datamodel.SITE_WIKIDATA);
//        wbde.disableEditing();


        try {
            ItemDocument item = (ItemDocument) dataFetcher.getEntityDocument(member.getQid());
            System.out.println("Label: = " + item.findLabel("lv"));
            StatementGroup statements = item.findStatementGroup("P39");
            statements.getStatements().forEach(statement -> {
                System.out.println("FOO: = " + statement);

//                statement.getClaim().getQualifiers().stream().filter(snak -> snak.getProperty().toString().endsWith("P582")).forEach(snaks -> System.out.println(">> " + snaks));

                statement.getClaim().getQualifiers().forEach(qual -> {
                    qual.getSnaks().forEach(snak -> {
                        System.out.println(">>>> " + snak.getPropertyId() + " = " + snak.getValue());
                    });
                });
            });

            List<Statement> addStat = new ArrayList<>();
            addStat.add(StatementBuilder.forSubjectAndProperty(Datamodel.makeWikidataItemIdValue(member.getQid()), Datamodel
                    .makeWikidataPropertyIdValue("P39")).withValue(Datamodel.makeWikidataItemIdValue("Q21191589"))


                    .build());

//            Snak snak = new ValueSnakImpl()


//            wbde.editItemDocument(item, false, "update nothing");
//            wbde.updateStatements(item, addStat, Collections.emptyList(), "add more");
        } catch (MediaWikiApiErrorException e) {
            e.printStackTrace();
        }


    }
}
