package btools.expressions;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertNotNull;

public class ParseWarningsProfilesTest {

  @Test
  public void testWarningsAreParsedAndDistinct() throws IOException {
    File workingDir = new File(".").getCanonicalFile();
    File profileDir = new File(workingDir, "../misc/profiles2/");
    File profile = new File(profileDir, "allWithWarn.brf");
    assertNotNull("Missing profile containing warnings", profile);
    BExpressionMetaData meta = new BExpressionMetaData();
    BExpressionContext expctxWay = new BExpressionContextWay(meta);
    BExpressionContext expctxNode = new BExpressionContextNode(meta);
    meta.readMetaData(new File(profileDir, "lookups.dat"));
    expctxWay.parseFile(profile, "global");
    expctxNode.parseFile(profile, "global");
    Assert.assertEquals(expctxNode.warnings, new HashSet<>(Arrays.asList("w4", "w5", "w6", "w8", "w7")));
    Assert.assertEquals(expctxWay.warnings, new HashSet<>(Arrays.asList("w3", "w1", "w2", "w0")));
  }
}
