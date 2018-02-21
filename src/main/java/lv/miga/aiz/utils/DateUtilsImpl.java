package lv.miga.aiz.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtilsImpl implements DateUtils {

    @Override
    public Date parseLatvianDate(String dateString) {
        if (!"".equals(dateString)) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
            try {
                return sdf.parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public String formatQuickStatementsDate(Date date) {
        if (date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("+yyyy-MM-dd'T'00:00:00'Z'/11");
            return sdf.format(date);
        }
        return null;
    }

    @Override
    public Date max(Date d1, Date d2) {
        if (d1 == null && d2 == null) return null;
        if (d1 == null) return d2;
        if (d2 == null) return d1;
        return (d1.after(d2)) ? d1 : d2;
    }

    @Override
    public Date min(Date d1, Date d2) {
        if (d1 == null && d2 == null) return null;
        if (d1 == null) return d2;
        if (d2 == null) return d1;
        return (d1.before(d2)) ? d1 : d2;
    }
}
