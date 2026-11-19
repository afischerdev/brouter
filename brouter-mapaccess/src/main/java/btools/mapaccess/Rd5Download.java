package btools.mapaccess;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.io.FileOutputStream;
import java.io.BufferedReader;
import java.io.FileReader;

import java.util.concurrent.atomic.AtomicBoolean;

import btools.expressions.BExpressionMetaData;
import btools.util.ProgressListener;

public final class Rd5Download {

  final public static String version = Rd5Download.class.getPackage().getImplementationVersion();

  final static boolean DEBUG = false;

  private static final String SEGMENT_DIFF_SUFFIX = ".df5";
  private static final String SEGMENT_SUFFIX = ".rd5";

  private static String segment_url = "https://brouter.de/brouter/segments4/";
  private static String lookup_url = "https://brouter.de/brouter/profiles2/";
  private static String profile_url = "https://brouter.de/brouter/profiles2/";

  private static String lookup_dat = "lookups.dat";
  private static String[] profile_list = {"car-eco.brf",
    "car-eco-de.brf",
    "car-fast.brf",
    "car-eco-nocost.brf",
    "car-fast-nocost.brf",
    "fastbike.brf",
    "fastbike-asia-pacific.brf",
    "fastbike-lowtraffic.brf",
    "fastbike-verylowtraffic.brf",
    "hiking-mountain.brf",
    "moped.brf",
    "mtb.brf",
    "ail.brf",
    "river.brf",
    "safety.brf",
    "shortest.brf",
    "trekking.brf",
    "trekking-ignore-cr.brf",
    "trekking-noferries.brf",
    "trekking-nosteps.brf",
    "trekking-steep.brf",
    "vm-forum-liegerad-schnell.brf",
    "vm-forum-velomobil-schnell.brf",
    "skating.brf"};

  public static boolean checkSystemUrl(String segmentBaseDir) {
    String segmentLocation = segment_url;
    try {
      URL segmentUrl = URI.create(segmentLocation).toURL();
      if (httpFileExists(segmentUrl)) {
        return true;
      }
      // check config file first
      if (loadConfig(segmentBaseDir)) {
        segmentUrl = URI.create(segment_url).toURL();
        if (httpFileExists(segmentUrl)) {
          return true;
        }
      }
      // get fallback
      if (getFallback(segmentBaseDir)) {
        segmentUrl = URI.create(segment_url).toURL();
        if (httpFileExists(segmentUrl)) {
          return true;
        }
      }
    } catch (Exception e) {
      System.out.println("checkSystemUrl error " + e.getMessage());
      return false;
    }
    return false;
  }

  private static boolean loadConfig(String segmentBaseDir) {
    File tmpFile = new File(segmentBaseDir, "serverconfig.txt");
    if (!tmpFile.exists()) return false;
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(tmpFile));
      String line;
      while ((line = br.readLine()) != null) {
        if (line.startsWith("segment_url="))
          segment_url = line.substring(12);
        if (line.startsWith("lookup_url="))
          lookup_url = line.substring(11);
        if (line.startsWith("profile_url="))
          profile_url = line.substring(12);
        if (line.startsWith("check_lookup="))
          lookup_dat = line.substring(13);
        if (line.trim().startsWith("check_profiles=")) {
          profile_list = line.substring(15).split(",");
        }
      }
      System.out.println("load fallback ");
      return true;
    } catch (Exception ignored) {
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException ignored) {
        }
      }
    }
    return false;
  }

  public static boolean getLookup(String profileBaseDir, AtomicBoolean change) {
    try {

      String fileName = lookup_dat;
      BExpressionMetaData meta = new BExpressionMetaData();
      int version = 0;
      int minorVersion = 0;
      int size = 0;
      File lookupFile = new File(profileBaseDir, fileName);
      if (lookupFile.exists()) {
        meta.readMetaData(lookupFile);
        version = meta.lookupVersion;
        minorVersion = meta.lookupMinorVersion;
        size = (int) lookupFile.length();
      }
      change.set(false);
      if (DEBUG) System.out.println("current version " + version +"." + minorVersion);
      File tmplookupFile = new File(profileBaseDir, fileName + ".tmp");

      if (tmplookupFile.exists()) {
        lookupFile.delete();
      }
      String lookupLocation = lookup_url + fileName;
      URL lookupUrl = URI.create(lookupLocation).toURL();
      boolean result = downloadFile(lookupUrl, tmplookupFile, size, false, 1, change);
      if (!result) {
        System.out.println("no result ");
        return false;
      }
      if (change.get()) { // true
        lookupFile.delete();
        tmplookupFile.renameTo(lookupFile);
        meta.readMetaData(lookupFile);
        version = meta.lookupVersion;
        minorVersion = meta.lookupMinorVersion;
        System.out.println("lookup change to version " + version +"." + minorVersion);
      } else {
        System.out.println("lookup no changes version " + version +"." + minorVersion);
        if (tmplookupFile.exists()) tmplookupFile.delete();
      }
    } catch (Throwable e) {
      System.out.println("unexpected exception: " + e);
      return false;
    }
    return true;
  }

  public static void getProfiles(String profileBaseDir) throws IOException, InterruptedException {
    String[] profiles = profile_list;
    for (String fileName : profiles) {
      if (fileName.length() > 0) {
        File profileFile = new File(profileBaseDir, fileName);
        if (profileFile.exists()) {
          profileFile.delete();
        }
        String profileLocation = profile_url + fileName;
        URL profileUrl = URI.create(profileLocation).toURL();
        int size = (int) (profileFile.exists() ? profileFile.length() : 0);

        try {
          downloadFile(profileUrl, profileFile, size, false, 2, new AtomicBoolean((false)));

        } catch (IOException e) {
          // no need to block other updates
        } catch (InterruptedException e) {
          throw new RuntimeException(e.getMessage());
        }

      }
    }
    System.out.println("profiles download");
  }


  private static boolean getFallback(String segmentBaseDir) {
    try {
      File tmpFile = new File(segmentBaseDir, "serverconfig.txt");
      String fallback_url = "https://raw.githubusercontent.com/abrensch/brouter/refs/heads/master/brouter-routing-app/src/main/assets/serverconfig.txt";
      URL tmpUrl = URI.create(fallback_url).toURL();
      boolean result = downloadFile(tmpUrl, tmpFile, 0, false, 0, new AtomicBoolean(false));
      if (result) {
        System.out.println("download fallback " + result);
        return loadConfig(segmentBaseDir);
      }
    } catch (Throwable e) {
      System.out.println("unexpected exception: " + e);
      return false;
    }
    return false;
  }

  public static boolean getRD5(String segmentBaseDir, String segmentName, AtomicBoolean change) {
    try {
      File segmentFile = new File(segmentBaseDir, segmentName + SEGMENT_SUFFIX);
      File tmpSegmentFile = new File(segmentFile.getAbsolutePath() + ".tmp");
      boolean result = false;

      if (segmentFile.exists() && !change.get()) {
        File tmpDeltaFile = new File(segmentBaseDir, segmentName + SEGMENT_SUFFIX + "_diff");
        if (tmpDeltaFile.exists()) {
          tmpDeltaFile.delete();
        }

        String md5 = Rd5DiffManager.getMD5(segmentFile);
        String segmentDeltaLocation = segment_url + "diff/" + segmentName + "/" + md5 + SEGMENT_DIFF_SUFFIX;
        URL segmentDeltaUrl = URI.create(segmentDeltaLocation).toURL();
        if (DEBUG) System.out.println("delta " + tmpDeltaFile.getAbsolutePath());
        if (DEBUG) System.out.println("delta " + segmentDeltaUrl);
        if (httpFileExists(segmentDeltaUrl)) {
          try {
            result = downloadFile(segmentDeltaUrl, tmpDeltaFile, 0, false, 0, new AtomicBoolean(false));
            if (DEBUG) System.out.println("download delta " + result);
            if (result) {
              ProgressListener diffProgressListener = new ProgressListener() {
                @Override
                public void updateProgress(String task, int progress) {
                  //if (DEBUG) System.out.println("updateProgress " + task + " " + progress);
                }

                @Override
                public boolean isCanceled() {
                  return false; // isStopped();
                }
              };
              Rd5DiffTool.recoverFromDelta(segmentFile, tmpDeltaFile, tmpSegmentFile, diffProgressListener);
            }
          } catch (IOException e) {
            throw new IOException("Failed to download & apply delta update", e);
          } finally {
            tmpDeltaFile.delete();
          }
        }
      } else {
        if (tmpSegmentFile.exists()) {
          tmpSegmentFile.delete();
        }
      }

      if (!tmpSegmentFile.exists()) {
        long t0 = System.currentTimeMillis();
        String segmentLocation = segment_url + segmentName + SEGMENT_SUFFIX;
        URL segmentUrl = URI.create(segmentLocation).toURL();
        result = downloadFile(segmentUrl, tmpSegmentFile, 0, false, 0, new AtomicBoolean(false));
        long t1 = System.currentTimeMillis();
        System.out.println(segmentName + " full download " +  (t1 - t0) + "ms");
      }
      if (result && tmpSegmentFile.exists()) {
        try {
          PhysicalFile.checkFileIntegrity(tmpSegmentFile);
          if (segmentFile.exists()) {
            if (!segmentFile.delete()) {
              throw new IOException("Failed to delete existing " + segmentFile.getAbsolutePath());
            }
          }
          if (!tmpSegmentFile.renameTo(segmentFile)) {
            throw new IOException("Failed to write " + segmentFile.getAbsolutePath());
          }

        } catch (Throwable e) {
          System.out.println("checkFileIntegrity exception: " + e);
          return false;
        }
      }
    } catch (Throwable e) {
      System.out.println("unexpected exception: " + e);
      return false;
    }
    return true;
  }

  private static boolean downloadFile(URL downloadUrl, File outputFile, int fileSize, boolean limitDownloadSpeed, int type, AtomicBoolean change) throws IOException, InterruptedException {
    if (DEBUG) System.out.println("download " + outputFile.getAbsolutePath());
    if (DEBUG) System.out.println("download " + downloadUrl);
    HttpURLConnection connection = null;
    InputStream input = null;
    OutputStream output = null;
    try {
      try {
        connection = (HttpURLConnection) downloadUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Rd5Download/" + version);
        connection.setConnectTimeout(5000);
        connection.setDefaultUseCaches(false);
        connection.connect();
      } catch (javax.net.ssl.SSLHandshakeException e) {
        String url = downloadUrl.toString().replace("https://", "http://");
        downloadUrl = URI.create(url).toURL();
        connection = (HttpURLConnection) downloadUrl.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Rd5Download/" + version);
        connection.setConnectTimeout(5000);
        connection.setDefaultUseCaches(false);
        connection.connect();
      }

      if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
        if (connection.getResponseCode() == 404) return false;
        throw new IOException("HTTP Request failed: " + downloadUrl + " returned " + connection.getResponseCode());
      }
      int dataLength = connection.getContentLength();

      // no need of download when size equal
      // file size not the best coice but easy to handle, date is not available
      switch (type) {
        case 1:
          if (fileSize != dataLength) {
            change.set(true);
          } else {
            return true;
          }
          break;
        case 2:
          if (fileSize == dataLength) {
            return true;
          }
          break;
        default:
          if (dataLength == 0) {
            return true;
          }
          break;
      }
      input = connection.getInputStream();
      output = new FileOutputStream(outputFile);

      byte[] buffer = new byte[4096];
      int total = 0;
      long t0 = System.currentTimeMillis();
      int count;
      while ((count = input.read(buffer)) != -1) {
        total += count;
        output.write(buffer, 0, count);

        if (limitDownloadSpeed) {
          // enforce < 16 Mbit/s
          long dt = t0 + total / 2096 - System.currentTimeMillis();
          if (dt > 0) {
            Thread.sleep(dt);
          }
        }
      }
    } catch (Exception e) {
      System.out.println("error: " + e.getMessage());
      return false;
    } finally {
      if (input != null) input.close();
      if (output != null) output.close();
      if (connection != null) connection.disconnect();
    }
    return true;
  }

  private static boolean httpFileExists(URL downloadUrl) throws IOException {
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) downloadUrl.openConnection();
      connection.setConnectTimeout(5000);
      connection.setRequestMethod("HEAD");
      connection.setRequestProperty("User-Agent", "Rd5Download/" + version);
      connection.setDoInput(true);
      connection.connect();
      if (connection.getResponseCode() != 200) System.out.println("httpFileExists resp " + connection.getResponseCode() + " for " + downloadUrl);
      return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
    } catch (javax.net.ssl.SSLHandshakeException e) {
      String url = downloadUrl.toString().replace("https://", "http://");
      downloadUrl = URI.create(url).toURL();
      try {
        connection = (HttpURLConnection) downloadUrl.openConnection();
        connection.setConnectTimeout(5000);
        connection.setRequestMethod("HEAD");
        connection.setRequestProperty("User-Agent", "Rd5Download/" + version);
        connection.setDoInput(true);
        connection.connect();
        if (connection.getResponseCode() != 200) System.out.println("httpFileExists resp " + connection.getResponseCode() + " for " + downloadUrl);
        return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
      } finally {
        if (connection != null) connection.disconnect();
      }
    } finally {
      if (connection != null) connection.disconnect();
    }

  }

}
