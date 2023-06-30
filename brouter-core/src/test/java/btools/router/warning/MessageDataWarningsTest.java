package btools.router.warning;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MessageDataWarningsTest {

  private static class TestResource {
    String n;
    String w;
    int sizeN;
    int sizeW;
    String message;

    public TestResource(String n, String w, int sizeN, int sizeW, String message) {
      this.n = n;
      this.w = w;
      this.sizeN = sizeN;
      this.sizeW = sizeW;
      this.message = message;
    }
  }

  @Test
  public void testParseIntoStructureAssertSizes() {
    List<TestResource> resources = new ArrayList<>();
    resources.add(new TestResource(null, null, 0, 0, "Empty for null values"));
    resources.add(new TestResource("", " ", 0, 0, "Empty for empty or blank values"));
    resources.add(new TestResource("key", "key-value", 0, 0, "Empty for not key=value"));
    resources.add(new TestResource("key", "key-value", 0, 0, "Empty for not key=value"));
    resources.add(new TestResource("key=value  \n\t", " key=value   key=value   ", 1, 1, "Expected 1"));
    for (TestResource tr : resources) {
      MessageDataWarnings w = new MessageDataWarnings(tr.n, tr.w);
      Assert.assertTrue(tr.message, w.nodeTags.size() == tr.sizeN && w.wayTags.size() == tr.sizeW);
    }
  }

  @Test
  public void testSpecificCase(){
    MessageDataWarnings w = new MessageDataWarnings("key=value  \n\t", " key0=value0   key0=value1  key1=value2 ");
    Assert.assertEquals("value", w.nodeTags.get("key"));
    Assert.assertEquals("value1", w.wayTags.get("key0"));
    Assert.assertEquals("value2", w.wayTags.get("key1"));
  }

  @Test
  public void testNoUnexpectedToken(){
    // eyeball test there is no warning printed for proper syntax
    MessageDataWarnings w = new MessageDataWarnings("key10=value10", "key20=value20 key20=value10 key100=value200");
    Assert.assertEquals(2, w.wayTags.size());
  }
}
