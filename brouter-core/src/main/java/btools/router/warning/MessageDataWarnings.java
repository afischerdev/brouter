package btools.router.warning;

import java.util.HashMap;
import java.util.Map;

class MessageDataWarnings {

  // maybe empty
  Map<String, String> nodeTags;
  // maybe empty
  Map<String, String> wayTags;

  MessageDataWarnings(String nodeKeyValues, String wayKeyValues) {
    nodeTags = buildTags(nodeKeyValues);
    wayTags = buildTags(wayKeyValues);
  }

  // convert wayKeyValues, nodeKeyValues into fast lookup data
  // sample of keyValues
  // highway=path sac_scale=mountain_hiking route_hiking_rwn=yes
  // we assume no multiple values for single key
  // TODO Is it possible to use "all values for a key" syntax in the
  // TODO lookup files? e.g. highway=*
  // TODO this will be necessary for e.g. a seasonal access parsing.
  // TODO Values like key=v1;v2;v3
  // TODO https://wiki.openstreetmap.org/wiki/Multiple_values
  // TODO If we specify 'processUnusedTags = true', will such a value
  // TODO be visible here?

  private static final String WHITESPACE_SEPARATOR = "[ ]";
  private static final String EQUALS_SEPARATOR = "[=]";

  private Map<String, String> buildTags(String keyValues) {
    Map<String, String> keyValuesMap = new HashMap<>();
    if (keyValues != null && !keyValues.isBlank()) {
      String[] tokens = keyValues.trim().split(WHITESPACE_SEPARATOR);
      for (String token : tokens) {
        String[] kv = token.split(EQUALS_SEPARATOR);
        if (kv.length == 2) {
          keyValuesMap.put(kv[0].trim(), kv[1].trim());
        } else {
          System.out.println("Unexpected token in: " + keyValues);
        }
      }
    }
    return keyValuesMap;
  }
}
