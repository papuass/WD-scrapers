package lv.miga.aiz.utils;

import java.util.Date;

public interface DateUtils {

    Date parseLatvianDate(String dateString);

    String formatQuickStatementsDate(Date date);

    Date max(Date d1, Date d2);

    Date min(Date d1, Date d2);

}
