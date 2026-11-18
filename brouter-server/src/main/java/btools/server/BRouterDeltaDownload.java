package btools.server;

import java.io.File;
import java.io.FileFilter;

import java.util.concurrent.atomic.AtomicBoolean;

import btools.mapaccess.Rd5Download;


public class BRouterDeltaDownload {

  final public static String version = BRouterDeltaDownload.class.getPackage().getImplementationVersion();

  private static final String SEGMENT_SUFFIX = ".rd5";

  private static boolean ignoreCheckProfile = Boolean.getBoolean("ignoreCheckProfile");

  public static void main(String[] args) {
    if (args.length == 3) {
      try {
        String segmentBaseDir = args[0];
        String profileBaseDir = args[1];
        String tileName = args[2];
        if (!Rd5Download.checkSystemUrl(segmentBaseDir)) {
          System.out.println("BRouterDeltaDownload wrong url");
          System.exit(0);
        }

        AtomicBoolean  change = new AtomicBoolean(false);

        if (ignoreCheckProfile || Rd5Download.getLookup(profileBaseDir, change)) {
          if (change.get()) Rd5Download.getProfiles(profileBaseDir);
          if (tileName.equals("all")) {
            getAll(segmentBaseDir, change);
          } else {
            System.out.println("BRouterDeltaDownload " + version + " - " + tileName);
            Rd5Download.getRD5(segmentBaseDir, tileName, change);
          }
        }

      } catch (Throwable e) {
        System.out.println("unexpected exception: " + e);
      }
      System.exit(0);
    } else {
      System.out.println("BRouterDeltaDownload " + version);
      System.out.println("Download rd5 delta");
      System.out.println("usage: java -cp brouter.jar btools.server.BRouterDeltaDownload <segmentdir> <profiledir> [<tileName>,all] ");
      System.out.println("       java -jar brouter_sync.jar <segmentdir> <profiledir> [<tileName>,all] ");
      System.exit(0);
    }

  }

  private static boolean getAll(String segmentBaseDir, AtomicBoolean change) {
    File segmentFolder = new File(segmentBaseDir);
    File[] files = segmentFolder.listFiles(new FileFilter() {
      @Override
      public boolean accept(File file) {
        return (file.getPath().endsWith(SEGMENT_SUFFIX));
      }
    });
    if (files != null) {
      for (File file : files) {
        String tileName = file.getName().substring(0, file.getName().indexOf(SEGMENT_SUFFIX));
        System.out.println("BRouterDeltaDownload " + version + " - " + tileName);
        boolean result = Rd5Download.getRD5(segmentBaseDir, tileName, change);
        if (!result) {
          System.out.println("download error on " + tileName);
          return false;
        }
      }
      return true;
    }
    return false;
  }


}
