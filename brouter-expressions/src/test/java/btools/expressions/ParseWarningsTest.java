package btools.expressions;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ParseWarningsTest {

  private static class TestPair {
    String sample;
    int expectedResultSize;

    public TestPair(String sample, int expectedResultSize) {
      this.sample = sample;
      this.expectedResultSize = expectedResultSize;
    }
  }

  @Test
  public void testParsedSamples() {

    // Expected syntax:
    // ---context:way
    // assign warnings = identifier1&identifier2
    // ...
    // ---context:node
    // assign warnings = identifier1&identifier2&identifier3

    List<TestPair> pairs = new ArrayList<>();
    pairs.add(new TestPair(null, 0));
    pairs.add(new TestPair("", 0));
    pairs.add(new TestPair(" ", 0));
    pairs.add(new TestPair("      ", 0));
    pairs.add(new TestPair("      w", 1));
    pairs.add(new TestPair("      w  ", 1));
    pairs.add(new TestPair("w1&w2", 2));
    pairs.add(new TestPair(" w1&w2  ", 2));
    pairs.add(new TestPair(" w1&w1 ", 1));
    pairs.add(new TestPair("&&&", 0));
    pairs.add(new TestPair(" &", 0));
    pairs.add(new TestPair("& & &  ", 0));
    pairs.add(new TestPair("&w1&", 1));
    pairs.add(new TestPair("w1,w2", 1));
    pairs.add(new TestPair("walk-hike.difficult.very", 1));

    for (TestPair tp : pairs) {
      System.out.println();
      System.out.println(">" + tp.sample + "<");
      Set<String> parsed = new BExpression(tp.sample).parseWarnings();
      System.out.println(parsed);
      Assert.assertEquals(parsed.size(), tp.expectedResultSize);
    }
  }
}
