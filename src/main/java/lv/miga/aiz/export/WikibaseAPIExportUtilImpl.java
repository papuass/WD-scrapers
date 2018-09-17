package lv.miga.aiz.export;

import lv.miga.aiz.config.WikidataMappings;
import lv.miga.aiz.model.MemberOfParliament;
import lv.miga.aiz.model.ParliamentaryGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.helpers.ItemDocumentBuilder;
import org.wikidata.wdtk.datamodel.helpers.ReferenceBuilder;
import org.wikidata.wdtk.datamodel.helpers.StatementBuilder;
import org.wikidata.wdtk.datamodel.interfaces.*;
import org.wikidata.wdtk.util.WebResourceFetcherImpl;
import org.wikidata.wdtk.wikibaseapi.ApiConnection;
import org.wikidata.wdtk.wikibaseapi.LoginFailedException;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataEditor;
import org.wikidata.wdtk.wikibaseapi.WikibaseDataFetcher;
import org.wikidata.wdtk.wikibaseapi.apierrors.MediaWikiApiErrorException;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.wikidata.wdtk.datamodel.helpers.Datamodel.*;

public class WikibaseAPIExportUtilImpl implements ExportUtil {

    private static final String USER_AGENT = "Saeima bot 0.0.1; Wikidata Toolkit; Java";

    private static final String ITEM_SANDBOX = "Q4115189";
    private static final String PROPERTY_BIRTH_YEAR = "P569";
    private static final String PROPERTY_CITIZENSHIP = "P27";
    private static final String PROPERTY_INSTANCE_OF = "P31";
    private static final String PROPERTY_DATE_FROM = "P580";
    private static final String PROPERTY_DATE_TO = "P582";
    private static final String PROPERTY_PARL_GROUP = "P4100";
    private static final String PROPERTY_PARL_TERM = "P2937";
    private static final String PROPERTY_POSITION = "P39";
    private static final String PROPERTY_REFERENCE_URL = "P854";
    private static final String PROPERTY_RETRIEVED = "P813";
    private static final String PROPERTY_PUBLISHER = "P123";
    private static final String PROPERTY_LANGUAGES = "P1412";
    private static final String PROPERTY_OCCUPATION = "P106";

    private static final String ITEM_MEMBER_OF_PERLIAMENT = "Q21191589";
    private static final String ITEM_SAEIMA = "Q822919";
    private static final String ITEM_HUMAN = "Q5";
    private static final String ITEM_LATVIA = "Q211";
    private static final String ITEM_LATVIAN_LANGUAGE = "Q9078";
    private static final String ITEM_POLITICIAN = "Q82955";

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void export(MemberOfParliament member) {
        ApiConnection connection = getWikidataConnection();

        WikibaseDataFetcher dataFetcher = new WikibaseDataFetcher(connection, Datamodel.SITE_WIKIDATA);
        dataFetcher.getFilter().excludeAllLanguages();
        dataFetcher.getFilter().excludeAllSiteLinks();

        String parliamentId = WikidataMappings.parliaments.get(member.getParliament());
        WikibaseDataEditor wbde = new WikibaseDataEditor(connection, Datamodel.SITE_WIKIDATA);
        try {
            if (member.getQid() == null) {
                ItemDocumentBuilder itemDocumentBuilder = ItemDocumentBuilder.forItemId(ItemIdValue.NULL)
                        .withLabel(formatName(member), "lv")
                        .withLabel(formatName(member), "en")
                        .withLabel(formatName(member), "de")
                        .withLabel(formatName(member), "fr")
                        .withStatement(StatementBuilder.forSubjectAndProperty(ItemIdValue.NULL, makeWikidataPropertyIdValue(PROPERTY_INSTANCE_OF)).withValue(makeWikidataItemIdValue(ITEM_HUMAN)).build())
                        .withStatement(StatementBuilder.forSubjectAndProperty(ItemIdValue.NULL, makeWikidataPropertyIdValue(PROPERTY_CITIZENSHIP)).withValue(makeWikidataItemIdValue(ITEM_LATVIA)).build())
                        .withStatement(StatementBuilder.forSubjectAndProperty(ItemIdValue.NULL, makeWikidataPropertyIdValue(PROPERTY_LANGUAGES)).withValue(makeWikidataItemIdValue(ITEM_LATVIAN_LANGUAGE)).build())
                        .withStatement(StatementBuilder.forSubjectAndProperty(ItemIdValue.NULL, makeWikidataPropertyIdValue(PROPERTY_OCCUPATION)).withValue(makeWikidataItemIdValue(ITEM_POLITICIAN)).build())
                        .withStatement(StatementBuilder.forSubjectAndProperty(ItemIdValue.NULL, makeWikidataPropertyIdValue(PROPERTY_BIRTH_YEAR)).withValue(
                                makeTimeValue(member.getBirthYear(), (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, TimeValue.PREC_YEAR, 0, 0, 0, TimeValue.CM_GREGORIAN_PRO)
                        ).withReference(createReference(member.getReferenceURL())).build());

                member.getParliamentaryGroups().forEach(group -> {
                    itemDocumentBuilder.withStatement(createNewStatement(member, parliamentId, group));
                });

                wbde.createItemDocument(itemDocumentBuilder.build(), "Creating new item for parliament member");
            } else {
                ItemDocument item = (ItemDocument) dataFetcher.getEntityDocument(member.getQid());
//                item = (ItemDocument) dataFetcher.getEntityDocument(ITEM_SANDBOX);

                List<Statement> statementsToAdd = new ArrayList<>();

                member.getParliamentaryGroups().forEach(group -> {
                            Optional<Statement> existingStatement = findParliamentStatementWithQualifiers(item, parliamentId, group);
                            if (existingStatement.isPresent()) {
                                Statement updatedStatement = updateStatement(member, existingStatement.get(), parliamentId, group);
                                statementsToAdd.add(updatedStatement);
                            } else {
                                // TODO: check for problem when creating more than one statement
                                Statement newStatement = createNewStatement(member, parliamentId, group);
                                statementsToAdd.add(newStatement);
                            }
                        }
                );
                wbde.updateStatements(item, statementsToAdd, Collections.emptyList(), "Updating parliament membership data");
            }
        } catch (MediaWikiApiErrorException | IOException e) {
            logger.error("Error while exporting data", e);
            e.printStackTrace();
        }
    }

    private String formatName(MemberOfParliament member) {
        return member.getName() + " " + member.getSurname();
    }

    private Statement updateStatement(MemberOfParliament member, Statement existingStatement, String parliamentId, ParliamentaryGroup group) {
        Reference reference = createReference(member.getReferenceURL());
        // TODO do not update data if only refernce date is changed
        StatementBuilder statementBuilder = StatementBuilder.forSubjectAndProperty(makeWikidataItemIdValue(member.getQid()),
                makeWikidataPropertyIdValue(PROPERTY_POSITION))
                .withId(existingStatement.getStatementId())
                .withValue(existingStatement.getValue())
                .withQualifiers(existingStatement.getClaim().getQualifiers().stream().filter(this::notInUpdatableProperties).collect(Collectors.toList()))
                .withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_PARL_TERM),
                        makeWikidataItemIdValue(parliamentId))
                .withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_PARL_GROUP),
                        makeWikidataItemIdValue(WikidataMappings.groups.get(group.getGroupName())))
                .withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_DATE_FROM), convertFromDate(group.getDateFrom()))
                .withReference(reference);
        if (group.getDateTo() != null) {
            statementBuilder.withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_DATE_TO), convertFromDate(group.getDateTo()));
        }
        return statementBuilder.build();
    }

    private boolean notInUpdatableProperties(SnakGroup snaks) {
        List<String> excludedProps = Arrays.asList(PROPERTY_DATE_TO, PROPERTY_DATE_FROM, PROPERTY_PARL_TERM, PROPERTY_PARL_GROUP);
        return !excludedProps.contains(snaks.getProperty().getId());
    }

    private Value convertFromDate(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return makeTimeValue(c.get(Calendar.YEAR), (byte) (c.get(Calendar.MONTH) + 1),
                (byte) c.get(Calendar.DATE), (byte) 0, (byte) 0, (byte) 0, TimeValue.PREC_DAY,
                0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO);
    }

    private Statement createNewStatement(MemberOfParliament member, String parliamentId, ParliamentaryGroup group) {
        Reference reference = createReference(member.getReferenceURL());

        ItemIdValue itemIdValue = member.getQid() == null ? ItemIdValue.NULL : makeWikidataItemIdValue(member.getQid());
        StatementBuilder statementBuilder = StatementBuilder.forSubjectAndProperty(itemIdValue,
                makeWikidataPropertyIdValue(PROPERTY_POSITION))
                .withValue(makeWikidataItemIdValue(ITEM_MEMBER_OF_PERLIAMENT))
                .withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_PARL_TERM),
                        makeWikidataItemIdValue(parliamentId))
                .withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_PARL_GROUP),
                        makeWikidataItemIdValue(WikidataMappings.groups.get(group.getGroupName())))
                .withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_DATE_FROM), convertFromDate(group.getDateFrom()))
                .withReference(reference);

        if (group.getDateTo() != null) {
            statementBuilder.withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_DATE_TO), convertFromDate(group.getDateTo()));
        }
        return statementBuilder.build();
    }

    private Reference createReference(String referenceURL) {
        return ReferenceBuilder.newInstance()
                .withPropertyValue(makeWikidataPropertyIdValue(PROPERTY_REFERENCE_URL), makeStringValue(referenceURL))
                .withPropertyValue(makeWikidataPropertyIdValue(PROPERTY_RETRIEVED), convertFromDate(new Date()))
                .withPropertyValue(makeWikidataPropertyIdValue(PROPERTY_PUBLISHER), makeWikidataItemIdValue(ITEM_SAEIMA))
                .build();
    }

    private Optional<Statement> findParliamentStatementWithQualifiers(ItemDocument item, String parliamentId, ParliamentaryGroup group) {
        Optional<Statement> foundStatement;
        StatementGroup statements = item.findStatementGroup(PROPERTY_POSITION);
        if (statements != null) {
            foundStatement = statements.getStatements().stream().filter(statement -> {
                Claim claim = statement.getClaim();

                boolean matchingTerm = claim.getQualifiers().stream().anyMatch(qual -> {
                    boolean matches = false;
                    PropertyIdValue propertyId = qual.getSnaks().get(0).getPropertyId();
                    if (propertyId.getId().equals(PROPERTY_PARL_TERM)) {
                        ItemIdValue value = (ItemIdValue) qual.getSnaks().get(0).getValue();
                        matches = value.getId().equals(parliamentId);
                    }
                    return matches;
                });
                boolean matchingStartDate = claim.getQualifiers().stream().anyMatch(qual -> {
                    boolean matches = false;
                    PropertyIdValue propertyId = qual.getSnaks().get(0).getPropertyId();
                    if (propertyId.getId().equals(PROPERTY_DATE_FROM)) {
                        TimeValue value = (TimeValue) qual.getSnaks().get(0).getValue();
                        matches = isSameDay(value, group.getDateFrom());
                    }
                    return matches;
                });
                boolean emptyStartDate = claim.getQualifiers().stream().noneMatch(qual -> qual.getSnaks().get(0).getPropertyId().getId().equals(PROPERTY_DATE_FROM));

                return matchingTerm && (emptyStartDate || matchingStartDate);
            }).findFirst();
        } else {
            foundStatement = Optional.empty();
        }
        return foundStatement;
    }

    private boolean isSameDay(TimeValue value, Date dateFrom) {
        Calendar c = Calendar.getInstance();
        c.setTime(dateFrom);
        return value.getYear() == c.get(Calendar.YEAR) && (value.getMonth() == c.get(Calendar.MONTH) + 1) && value.getDay() == c.get(Calendar.DATE);
    }

    private ApiConnection getWikidataConnection() {
        WebResourceFetcherImpl.setUserAgent(USER_AGENT);
        ApiConnection connection = ApiConnection.getWikidataApiConnection();
        String username = System.getProperty("username");
        String password = System.getProperty("password");
        if (password == null || username == null) {
            System.out.println("Run with -Dusername=<username> -Dpassword=<password>");
        } else {
            try {
                connection.login(username, password);
            } catch (LoginFailedException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
}
