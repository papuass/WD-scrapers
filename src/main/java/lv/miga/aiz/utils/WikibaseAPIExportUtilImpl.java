package lv.miga.aiz.utils;

import lv.miga.aiz.model.MemberOfParliament;
import lv.miga.aiz.model.ParliamentaryGroup;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
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
    private static final Map<Integer, String> parliamentConfig;
    private static final Map<String, String> groupConfig;
    public static final String ITEM_MEMBER_OF_PERLIAMENT = "Q21191589";
    public static final String ITEM_SAEIMA = "Q822919";
    public static final String PROPERTY_DATE_FROM = "P580";
    public static final String PROPERTY_DATE_TO = "P582";
    public static final String PROPERTY_PARL_GROUP = "P4100";
    public static final String PROPERTY_PARL_TERM = "P2937";
    public static final String PROPERTY_POSITION = "P39";
    public static final String PROPERTY_REFERENCE_URL = "P854";
    public static final String PROPERTY_RETRIEVED = "P813";
    public static final String PROPERTY_PUBLISHER = "P123";

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(12, "Q20557340");
        map.put(11, "Q13098708");
        map.put(10, "Q16347625");
        parliamentConfig = Collections.unmodifiableMap(map);
    }

    static {
        Map<String, String> map = new HashMap<>();
        map.put("For Latvia from the Heart parliamentary group", "Q49638223");
        map.put("National Alliance \"All For Latvia!\" – \"For Fatherland and Freedom/LNNK\" parliamentary group", "Q49637732");
        map.put("National Alliance of All for Latvia! and For Fatherland and Freedom/LNNK parliamentary group", "Q49637732");
        map.put("Concord parliamentary group", "Q49637655");
        map.put("Unity parliamentary group", "Q49636278");
        map.put("Union of Greens and Farmers parliamentary group", "Q49636011");
        map.put("Latvian Regional Alliance parliamentary group", "Q49637927");
        map.put("Unaffiliated members of parliament", "Q49638041");
        groupConfig = Collections.unmodifiableMap(map);
    }


    @Override
    public void export(MemberOfParliament member) {
        ApiConnection connection = getWikidataConnection();

        WikibaseDataFetcher dataFetcher = new WikibaseDataFetcher(connection, Datamodel.SITE_WIKIDATA);
        dataFetcher.getFilter().excludeAllLanguages();
        dataFetcher.getFilter().excludeAllSiteLinks();

        try {
            ItemDocument item = (ItemDocument) dataFetcher.getEntityDocument(member.getQid());
//            ItemDocument item = (ItemDocument) dataFetcher.getEntityDocument("Q4115189"); // sandbox
            System.out.println("Label: = " + item.findLabel("lv"));

            List<Statement> statementsToAdd = new ArrayList<>();
            String parliamentId = parliamentConfig.get(member.getParliament());
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

            WikibaseDataEditor wbde = new WikibaseDataEditor(connection, Datamodel.SITE_WIKIDATA);
//            wbde.editItemDocument(item, false, "update nothing");
            wbde.updateStatements(item, statementsToAdd, Collections.emptyList(), "Updating parliament membership data");
        } catch (MediaWikiApiErrorException | IOException e) {
            e.printStackTrace();
        }
    }

    private Statement updateStatement(MemberOfParliament member, Statement existingStatement, String parliamentId, ParliamentaryGroup group) {
        Reference reference = createReference(member);
        StatementBuilder statementBuilder = StatementBuilder.forSubjectAndProperty(makeWikidataItemIdValue(member.getQid()),
                makeWikidataPropertyIdValue(PROPERTY_POSITION))
                .withId(existingStatement.getStatementId())
                .withValue(existingStatement.getValue())
                .withQualifiers(existingStatement.getClaim().getQualifiers().stream().filter(this::notInUpdatableProperties).collect(Collectors.toList()))
                .withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_PARL_TERM),
                        makeWikidataItemIdValue(parliamentId))
                .withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_PARL_GROUP),
                        makeWikidataItemIdValue(groupConfig.get(group.getGroupName())))
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

    private Value convertFromDate(Date dateFrom) {
        Calendar c = Calendar.getInstance();
        c.setTime(dateFrom);
        return makeTimeValue(c.get(Calendar.YEAR), (byte) (c.get(Calendar.MONTH) + 1),
                (byte) c.get(Calendar.DATE), (byte) 0, (byte) 0, (byte) 0, TimeValue.PREC_DAY,
                0, 0, 0,
                TimeValue.CM_GREGORIAN_PRO);
    }

    private Statement createNewStatement(MemberOfParliament member, String parliamentId, ParliamentaryGroup group) {
        Reference reference = createReference(member);

        StatementBuilder statementBuilder = StatementBuilder.forSubjectAndProperty(makeWikidataItemIdValue(member.getQid()),
                makeWikidataPropertyIdValue(PROPERTY_POSITION))
                .withValue(makeWikidataItemIdValue(ITEM_MEMBER_OF_PERLIAMENT))
                .withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_PARL_TERM),
                        makeWikidataItemIdValue(parliamentId))
                .withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_PARL_GROUP),
                        makeWikidataItemIdValue(groupConfig.get(group.getGroupName())))
                .withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_DATE_FROM), convertFromDate(group.getDateFrom()))
                .withReference(reference);

        if (group.getDateTo() != null) {
            statementBuilder.withQualifierValue(makeWikidataPropertyIdValue(PROPERTY_DATE_TO), convertFromDate(group.getDateTo()));
        }
        return statementBuilder.build();
    }

    private Reference createReference(MemberOfParliament member) {
        return ReferenceBuilder.newInstance()
                .withPropertyValue(makeWikidataPropertyIdValue(PROPERTY_REFERENCE_URL), makeStringValue(member.getReferenceURL()))
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
