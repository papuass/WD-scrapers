package lv.miga.aiz.export;

import com.google.inject.Inject;
import lv.miga.aiz.config.WikidataMappings;
import lv.miga.aiz.model.MemberOfParliament;
import lv.miga.aiz.model.ParliamentaryGroup;
import lv.miga.aiz.utils.DateUtils;
import lv.miga.aiz.utils.TextUtils;

public class QuickStatementsV1ExportUtilImpl implements ExportUtil {

    private static final String TAB = "\t";
    private static final String POSITION = "P39";
    private static final String DEPUTY_OF_SAEIMA = "Q21191589";
    private static final String TERM = "P2937";
    private static final String START = "P580";
    private static final String END = "P582";
    private static final String PARL_GROUP = "P4100";
    private static final String URL = "S854";
    private static final String CHECKED_DATE = "S813";

    private DateUtils dateUtils;
    private TextUtils textUtils;

    @Inject
    public QuickStatementsV1ExportUtilImpl(DateUtils dateUtils, TextUtils textUtils) {
        this.dateUtils = dateUtils;
        this.textUtils = textUtils;
    }

    @Override
    public void export(MemberOfParliament member) {
        StringBuilder sb = new StringBuilder();

        String qid;
        if (member.getQid() == null) {
            sb.append("CREATE\n");
            qid = "LAST";
        } else {
            qid = member.getQid();
        }
        member.getParliamentaryGroups().forEach(parliamentaryGroup -> sb.append(processFraction(qid, member, parliamentaryGroup)));

        System.out.println(sb);
        System.out.println();
        System.out.println(createUploadURL(sb.toString()));
    }

    private String processFraction(String qid, MemberOfParliament member, ParliamentaryGroup parliamentaryGroup) {
        StringBuilder sb = new StringBuilder();

        sb.append(qid).append(TAB).append(POSITION).append(TAB).append(DEPUTY_OF_SAEIMA).append(TAB).append(TERM).append(TAB)
                .append(WikidataMappings.parliaments.get(member.getParliament())).append(TAB).append(PARL_GROUP).append(TAB)
                .append(WikidataMappings.groups.get(parliamentaryGroup.getGroupName())).append(TAB).append(START).append(TAB)
                .append(dateUtils.formatQuickStatementsDate(parliamentaryGroup.getDateFrom()));
        if (parliamentaryGroup.getDateTo() != null) {
            sb.append(TAB + END + TAB).append(dateUtils.formatQuickStatementsDate(parliamentaryGroup.getDateTo()));
        }
        sb.append(TAB + URL + TAB).append(textUtils.doubleQuote(member.getReferenceURL()))
                // to allow multiple runs, let's not add checked date
                // .append(TAB).append(CHECKED_DATE).append(TAB).append(dateUtils.formatQuickStatementsDate(new Date()))
                .append("\n");
        return sb.toString();
    }

    private String createUploadURL(String commands) {
        // TODO: not compatible with Titania URLs
        return "https://tools.wmflabs.org/quickstatements/#v1=" + commands.replace("\t", "%09").replace("\"", "%22").replace("_", "%5F").replace("&", "%26").replace("=", "%3D")
                .replace(" ", "%20").replace("\n", "%0A");
    }
}
