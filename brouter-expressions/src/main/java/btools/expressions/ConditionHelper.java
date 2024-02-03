package btools.expressions;

import java.time.Instant;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ConditionHelper {

  final public static byte COND_TYPE_NONE = 0;
  final public static byte COND_TYPE_TIMES = 1;
  final public static byte COND_TYPE_WEEKDAYS = 2;
  final public static byte COND_TYPE_MONTHS = 3;
  final public static byte COND_TYPE_MONTH_DATES = 4;

  final public static byte COND_RESULT_INVALID = 0;
  final public static byte COND_RESULT_FALSE = 1;
  final public static byte COND_RESULT_TRUE = 2;

  final static String[] weekdays = {"Mo", "Tu", "We", "Th", "Fr", "Sa", "Su", "PH"};
  final static String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

  private static DateTimeFormatter longFormatter =
    DateTimeFormatter.ofPattern("yyyy LLL dd", Locale.US);
  private static DateTimeFormatter shortFormatter =
    DateTimeFormatter.ofPattern("LLLdd", Locale.US);

  public static byte getConditionalType(String s) {
    int pos = s.indexOf("(");
    int endpos = s.indexOf(")");
    if (pos == -1 || endpos == -1) {
      return COND_TYPE_NONE;
    }
    s = s.substring(pos + 1, endpos);
    if (s.contains(":") && s.contains("-")) {
      return COND_TYPE_TIMES;
    }
    for (String d : weekdays) {
      if (s.contains(d)) return COND_TYPE_WEEKDAYS;
    }
    boolean bWithDate = s.matches(".*\\d.*");
    for (String d : months) {
      if (s.contains(d)) return (bWithDate ? COND_TYPE_MONTH_DATES : COND_TYPE_MONTHS);
    }
    return COND_TYPE_NONE;
  }


  public static int getConditionForDate(byte type, long d, String value) {
    int result = COND_RESULT_INVALID;
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(d);
    //int day = c.get(Calendar.DAY_OF_MONTH);
    int month = c.get(Calendar.MONTH);
    int year = c.get(Calendar.YEAR);
    LocalDate date = Instant.ofEpochMilli(d).atZone(ZoneId.systemDefault()).toLocalDate();
    month = (1 << month);
    try {
      switch (type) {
        case COND_TYPE_MONTHS: {
          boolean fromto = false;
          int fromIdx = -1;
          int toIdx = -1;
          String sresult = value;
          sresult = sresult.replace("(", "");
          sresult = sresult.replace(")", "");
          String[] sa = sresult.trim().split("-");
          if (sa.length == 2) {
            List<String> list = Arrays.asList(months);
            fromIdx = list.indexOf(sa[0].trim());
            toIdx = list.indexOf(sa[1].trim());
            if (toIdx < fromIdx) toIdx += 12;
            for (int i = fromIdx; i <= toIdx; i++) {
              result += (1 << (i > 11 ? i - 12 : i));
            }
          } else {
            for (int i = 0; i < months.length; i++) {
              String s = months[i];
              if (sresult.contains(s)) {

                result += (1 << i);
              }
            }
          }

          result = ((result & month) > 0) ? COND_RESULT_TRUE : COND_RESULT_FALSE;

        }
        break;
        case COND_TYPE_MONTH_DATES: {
          String sresult = value;
          sresult = sresult.replace("(", "");
          sresult = sresult.replace(")", "");
          String[] sa = sresult.trim().split("-");
          LocalDate dateFrom = null;
          LocalDate dateTo = null;
          boolean bLongDate = false;
          if (sa.length == 2) {
            try {
              dateFrom = LocalDate.parse(sa[0].trim(), longFormatter);
              dateTo = LocalDate.parse(sa[1].trim(), longFormatter);
              bLongDate = true;
            } catch (Exception e) {
              // System.out.println("date e " + e.getMessage());
              try {
                MonthDay mdateFrom = MonthDay.parse(sa[0].replace(" ", ""), shortFormatter);
                MonthDay mdateTo = MonthDay.parse(sa[1].replace(" ", ""), shortFormatter);
                dateTo = mdateTo.atYear(year);
                dateFrom = mdateFrom.atYear(year);
                if (mdateFrom.isAfter(mdateTo)) {
                  if (date.isBefore(dateTo)) {
                    dateFrom = mdateFrom.atYear(year - 1);
                  } else {
                    dateTo = mdateTo.atYear(year + 1);
                  }
                }
                //System.out.println("date " + dateFrom + " - " + dateTo + " " + dateFrom.isAfter(dateTo));
              } catch (Exception exc) {
                System.out.println("error: " + exc.getMessage());
                result = COND_RESULT_INVALID;
              }
            }
            if (dateFrom != null && dateTo != null) {
              if (bLongDate && date.isAfter(dateFrom) && date.isAfter(dateTo)) {
                //System.out.println("date passed ");
                result = COND_RESULT_INVALID;
              } else {
                //System.out.println("date between " + (date.isAfter(dateFrom) && date.isBefore(dateTo)) + "   after " + date.isAfter(dateFrom) + " before " + date.isBefore(dateTo));
                result = (date.isAfter(dateFrom) && date.isBefore(dateTo)) ? COND_RESULT_TRUE : COND_RESULT_FALSE;
              }
            }
          }
        }
        break;
        default: break;
      }
    } catch (Exception e) {}
    return result;
  }

}
