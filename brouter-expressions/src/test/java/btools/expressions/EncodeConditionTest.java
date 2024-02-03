package btools.expressions;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;


public class EncodeConditionTest {

  @Test
  public void encodeDecodeTest() {
    URL testpurl = this.getClass().getResource("/dummy.txt");
    File workingDir = new File(testpurl.getFile()).getParentFile();
    File profileDir = new File(workingDir, "/../../../../misc/profiles2");
    URL testlookup = this.getClass().getResource("/lookups_next_test.dat");
    File lookupFile = new File(testlookup.getPath());

    // read lookup.dat + trekking.brf
    BExpressionMetaData meta = new BExpressionMetaData();
    BExpressionContextWay expctxWay = new BExpressionContextWay(meta);
    meta.readMetaData(lookupFile);
    expctxWay.parseFile(new File(profileDir, "trekking.brf"), "global");

    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(System.currentTimeMillis());
    c.set(Calendar.MONTH, Calendar.OCTOBER);
    c.set(Calendar.DAY_OF_MONTH, 20);

    expctxWay.setDate(c.getTimeInMillis());

    String[] tags = {
      "highway=residential",
      "oneway=yes",
      "oneway:conditional=no @ (18:00-07:00)",
      "access=yes",
      "access:conditional=no @ (Oct,Dec)",
      "reversedirection=yes",
      "maxspeed=30",
      "bicycle=yes",
      "bicycle:conditional=no @ (Dec-Apr)",
//    "foot:conditional=yes @ (Apr-Sep)"
      "foot:conditional=yes @ (Apr-Oct)"
    };

    // encode the tags into 64 bit description word
    int[] lookupData = expctxWay.createNewLookupData();
    long id = 100;
    BExpressionLookupValue condition = null;
    for (String arg : tags) {
      int idx = arg.indexOf('=');
      if (idx < 0)
        throw new IllegalArgumentException("bad argument (should be <tag>=<value>): " + arg);
      String key = arg.substring(0, idx);
      String value = arg.substring(idx + 1);

      BExpressionLookupValue nv = expctxWay.addLookupValue(key, value, lookupData);
    }

    byte[] description = expctxWay.encode(lookupData);

    // calculate the cost factor from that description
    expctxWay.evaluate(true, description); // true = "reversedirection=yes"  (not encoded in description anymore)

    System.out.println("description " + new Date(expctxWay.getDate()) + ":\n" + expctxWay.getKeyValueDescription(true, description));

    float costfactor = expctxWay.getCostfactor();
    Assert.assertTrue("costfactor mismatch", Math.abs(costfactor - 5.15) < 0.00001);
  }

  @Test
  public void testConditionValues() {
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(System.currentTimeMillis());
    c.set(Calendar.YEAR, 2023);
    c.set(Calendar.MONTH, Calendar.OCTOBER);
    c.set(Calendar.DAY_OF_MONTH, 14);

    String[] conditions = {
      "",
      "(18:00-07:00)",
      "(Jan-Mar)",
      "(Jan - Mar)",
      "(Jan -Mar)",
      "(Jan- Mar)",
      "(Apr-Oct)",
      "(Oct-Apr)",
      "(Apr,Oct)",
      "March to October",
      "(Apr)",
      "(Oct)",
      "(Mar15-Jun15)",
      "(Mar 15-Jun 15)",
      "(Apr 15-Oct 15)",
      "(Oct 15-Apr 15)",
      "(Oct 01-Apr 15)",
      "(2021 Nov 26-2022 Dec 31)",
      "(2022 Nov 26-2023 Dec 31)",
      "(Apr 14-Oct 31 AND stationary noise>95)"
    };

    System.out.println("---\ntest: " + c.getTime());
    for (String condition : conditions) {
      byte type = ConditionHelper.getConditionalType(condition);
      int result = ConditionHelper.getConditionForDate(type, c.getTimeInMillis(), condition);
      String sresult = (result == ConditionHelper.COND_RESULT_INVALID ? "invalid" :
        result == ConditionHelper.COND_RESULT_TRUE ? "true" : "false");
      System.out.println("test " + sresult + " type " + type + " for [" + condition + "]");
      Assert.assertTrue("condition result wrong", result < 3);
    }
  }
}
