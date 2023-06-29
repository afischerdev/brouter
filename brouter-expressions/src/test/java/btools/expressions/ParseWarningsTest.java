package btools.expressions;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class ParseWarningsTest {

  @Test
  public void testParsedSamples() {
    // Expected syntax:
    // ---context:way
    // assign warnings = identifier1&identifier2
    // ...
    // ---context:node
    // assign warnings = identifier1&identifier2&identifier3
    String[] samples = new String[]{null, "", " ", "   ", " w", " w ", "w1&w2", " w1&w2 ", " w1&w1 ", "&&&", " &", "& & &  ", "&w1&", "w1,w2", "walk-hike.difficult.very"};
    int[] expectedResultsSizes = new int[]{0, 0, 0, 0, 1, 1, 2, 2, 1, 0, 0, 0, 1, 1, 1};
    Assert.assertEquals("Given resources size mismatch", samples.length, expectedResultsSizes.length);
    for (int i = 0; i < samples.length; i++) {
      System.out.println();
      System.out.println(">" + samples[i] + "<");
      Set<String> parsed = new BExpression(samples[i]).parseWarnings();
      System.out.println(parsed);
      Assert.assertEquals(parsed.size(), expectedResultsSizes[i]);
    }
  }
}
