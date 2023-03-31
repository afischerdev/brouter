package btools.routingapp;

import static btools.routingapp.BInstallerView.MASK_CURRENT_RD5;
import static btools.routingapp.BInstallerView.MASK_DELETED_RD5;
import static btools.routingapp.BInstallerView.MASK_INSTALLED_RD5;
import static btools.routingapp.BInstallerView.MASK_SELECTED_RD5;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import btools.router.RoutingHelper;

public class BInstallerActivity extends AppCompatActivity {

  private static final int DIALOG_CONFIRM_DELETE_ID = 1;
  private static final int DIALOG_CONFIRM_NEXTSTEPS_ID = 2;
  private static final int DIALOG_CONFIRM_GETDIFFS_ID = 3;

  public static final int MY_PERMISSIONS_REQUEST_NITIFICATION = 100;

  public static boolean downloadCanceled = false;
  private File mBaseDir;
  private BInstallerView mBInstallerView;
  private Button mButtonDownload;
  private TextView mSummaryInfo;
  private TextView mDownloadSummaryInfo;
  private LinearProgressIndicator mProgressIndicator;
  private ArrayList<Integer> selectedTiles;

  BInstallerView.OnSelectListener onSelectListener;

  @SuppressWarnings("deprecation")
  public static long getAvailableSpace(String baseDir) {
    StatFs stat = new StatFs(baseDir);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
      return stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
    } else {
      //noinspection deprecation
      return (long) stat.getAvailableBlocks() * stat.getBlockSize();
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    boolean running = isDownloadRunning(DownloadWorker.class);

    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

    setContentView(R.layout.activity_binstaller);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.POST_NOTIFICATIONS)
        == PackageManager.PERMISSION_GRANTED) {
        // nothing to do
      }
      if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
        //
      } else {
        // You can directly ask for the permission.
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, MY_PERMISSIONS_REQUEST_NITIFICATION);
      }
    }

    mSummaryInfo = findViewById(R.id.textViewSegmentSummary);
    mBInstallerView = findViewById(R.id.BInstallerView);
    onSelectListener = new BInstallerView.OnSelectListener() {
      @Override
      public void onSelect() {
        //if (!isDownloadRunning(DownloadWorker.class))
        updateDownloadButton();
      }
    };

    mBInstallerView.setOnSelectListener(onSelectListener);

    mButtonDownload = findViewById(R.id.buttonDownload);
    mButtonDownload.setOnClickListener(
      view -> {
        if (isDownloadRunning(DownloadWorker.class)) {
          stopDownload();
        } else if (mBInstallerView.getSelectedTiles(MASK_DELETED_RD5).size() > 0) {
          showConfirmDelete();
        } else if (mBInstallerView.getSelectedTiles(MASK_SELECTED_RD5).size() > 0) {
          mBInstallerView.setOnSelectListener(null);
          downloadSelectedTiles();
        } else {
          mBInstallerView.setOnSelectListener(null);
          downloadInstalledTiles();
        }
      }
    );
    mProgressIndicator = findViewById(R.id.progressDownload);
    mDownloadSummaryInfo = findViewById(R.id.textViewDownloadSummary);
    mDownloadSummaryInfo.setVisibility(View.INVISIBLE);

    mBaseDir = ConfigHelper.getBaseDir(this);

    if (running) {
      mProgressIndicator.show();
      mButtonDownload.setEnabled(false);
      WorkManager instance = WorkManager.getInstance(getApplicationContext());
      LiveData<List<WorkInfo>> ld = instance.getWorkInfosForUniqueWorkLiveData(DownloadWorker.WORKER_NAME);
      ld.observe(this, listOfWorkInfo -> {
        // If there are no matching work info, do nothing
        if (listOfWorkInfo == null || listOfWorkInfo.isEmpty()) {
          return;
        }
        for (WorkInfo workInfo : listOfWorkInfo) {
          startObserver(workInfo);
        }

      });

    } else {
      scanExistingFiles();
    }
  }

  private String getSegmentsPlural(int count) {
    Resources res = getResources();
    return res.getQuantityString(R.plurals.numberOfSegments, count, count);
  }

  private void updateDownloadButton() {
    final ArrayList<Integer> selectedTilesDownload = mBInstallerView.getSelectedTiles(MASK_SELECTED_RD5);
    final ArrayList<Integer> selectedTilesLastUpdate = mBInstallerView.getSelectedTiles(MASK_CURRENT_RD5);
    final ArrayList<Integer> selectedTilesUpdate = mBInstallerView.getSelectedTiles(MASK_INSTALLED_RD5);
    final ArrayList<Integer> selectedTilesDelete = mBInstallerView.getSelectedTiles(MASK_DELETED_RD5);
    selectedTilesUpdate.removeAll(selectedTilesLastUpdate);
    mSummaryInfo.setText("");

    if (selectedTilesDelete.size() > 0) {
      mButtonDownload.setText(getString(R.string.action_delete, getSegmentsPlural(selectedTilesDelete.size())));
      mButtonDownload.setEnabled(true);
    } else if (selectedTilesDownload.size() > 0) {
      long tileSize = 0;
      for (int tileIndex : selectedTilesDownload) {
        tileSize += BInstallerSizes.getRd5Size(tileIndex);
      }
      mButtonDownload.setText(getString(R.string.action_download, getSegmentsPlural(selectedTilesDownload.size())));
      mButtonDownload.setEnabled(true);
      mSummaryInfo.setText(getString(R.string.summary_segments, Formatter.formatFileSize(this, tileSize), Formatter.formatFileSize(this, getAvailableSpace(mBaseDir.getAbsolutePath()))));
    } else if (selectedTilesUpdate.size() > 0) {
      mButtonDownload.setText(getString(R.string.action_update, getSegmentsPlural(selectedTilesUpdate.size())));
      mButtonDownload.setEnabled(true);
    } else {
      mButtonDownload.setText(getString(R.string.action_select));
      mButtonDownload.setEnabled(false);
    }
  }

  private void deleteRawTracks() {
    File modeDir = new File(mBaseDir, "brouter/modes");
    String[] fileNames = modeDir.list();
    if (fileNames == null) return;
    for (String fileName : fileNames) {
      if (fileName.endsWith("_rawtrack.dat")) {
        File f = new File(modeDir, fileName);
        f.delete();
      }
    }
  }

  public void downloadAll(ArrayList<Integer> downloadList, int all) {
    ArrayList<String> urlparts = new ArrayList<>();
    for (Integer i : downloadList) {
      urlparts.add(baseNameForTile(i));
    }

    downloadCanceled = false;
    mProgressIndicator.show();
    mButtonDownload.setEnabled(false);

    Data inputData = new Data.Builder()
      .putStringArray(DownloadWorker.KEY_INPUT_SEGMENT_NAMES, urlparts.toArray(new String[0]))
      .putInt(DownloadWorker.KEY_INPUT_SEGMENT_ALL, all)
      .build();

    Constraints constraints = new Constraints.Builder()
      .setRequiresBatteryNotLow(true)
      .setRequiresStorageNotLow(true)
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .build();

    WorkRequest downloadWorkRequest =
      new OneTimeWorkRequest.Builder(DownloadWorker.class)
        .setInputData(inputData)
        .setConstraints(constraints)
        .build();

    WorkManager workManager = WorkManager.getInstance(getApplicationContext());
    workManager.enqueueUniqueWork(DownloadWorker.WORKER_NAME, ExistingWorkPolicy.KEEP, (OneTimeWorkRequest) downloadWorkRequest);

    try {
      WorkInfo wi = WorkManager.getInstance(getApplicationContext()).getWorkInfoById(downloadWorkRequest.getId()).get();
      if (wi != null && (wi.getState() == WorkInfo.State.ENQUEUED || wi.getState() == WorkInfo.State.BLOCKED)) {
        Log.d("worker", "cancel " + wi.getState());
        //WorkManager.getInstance(getApplicationContext()).cancelWorkById(downloadWorkRequest.getId());
      }
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      Log.d("worker", "canceled " + e.getMessage());
      //e.printStackTrace();
    }

    workManager
      .getWorkInfoByIdLiveData(downloadWorkRequest.getId())
      .observe(this, workInfo -> {
        startObserver(workInfo);
      });

    deleteRawTracks(); // invalidate raw-tracks after data update
  }

  private void startObserver(WorkInfo workInfo) {
    if (workInfo != null) {
      if (workInfo.getState() == WorkInfo.State.ENQUEUED || workInfo.getState() == WorkInfo.State.BLOCKED) {
        Log.d("worker", "cancel " + workInfo.getState());
        //WorkManager.getInstance(getApplicationContext()).cancelWorkById(downloadWorkRequest.getId());
      }

      if (workInfo.getState() == WorkInfo.State.ENQUEUED) {
        Toast.makeText(this, "Download scheduled. Check internet connection if it doesn't start.", Toast.LENGTH_LONG).show();
        mProgressIndicator.hide();
        mProgressIndicator.setIndeterminate(true);
        mProgressIndicator.show();
      }

      if (workInfo.getState() == WorkInfo.State.RUNNING) {
        mDownloadSummaryInfo.setVisibility(View.VISIBLE);
        Data progress = workInfo.getProgress();
        String segmentName = progress.getString(DownloadWorker.PROGRESS_SEGMENT_NAME);
        int percent = progress.getInt(DownloadWorker.PROGRESS_SEGMENT_PERCENT, 0);
        if (percent > 0) {
          mDownloadSummaryInfo.setText("Downloading .. " + segmentName);
        }
        if (percent > 0) {
          mProgressIndicator.setIndeterminate(false);
        }
        mProgressIndicator.setProgress(percent);

        mButtonDownload.setText(getString(R.string.action_cancel));
        mButtonDownload.setEnabled(true);

      }

      if (workInfo.getState().isFinished()) {
        String result;
        switch (workInfo.getState()) {
          case FAILED:
            result = "Download failed";
            break;
          case CANCELLED:
            result = "Download cancelled";
            break;
          case SUCCEEDED:
            result = "Download succeeded";
            break;
          default:
            result = "";
        }
        String error = null;
        if (workInfo.getState() != WorkInfo.State.FAILED) {
          Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        } else {
          error = workInfo.getOutputData().getString(DownloadWorker.KEY_OUTPUT_ERROR);
          if (error != null && !error.startsWith("Version")) {
            Toast.makeText(this, result + ": " + error, Toast.LENGTH_LONG).show();
          }
        }

        if (error != null && error.startsWith("Version error")) {
          showConfirmNextSteps();
        } else if (error != null && error.startsWith("Version diffs")) {
          showConfirmGetDiffs();
        } else {
          mBInstallerView.setOnSelectListener(onSelectListener);
          mBInstallerView.clearAllTilesStatus(MASK_SELECTED_RD5);
          scanExistingFiles();
        }
        mProgressIndicator.hide();
        mDownloadSummaryInfo.setVisibility(View.INVISIBLE);
        mButtonDownload.setEnabled(true);
      }
    }

  }


  protected Dialog createADialog(int id) {
    AlertDialog.Builder builder;
    builder = new AlertDialog.Builder(this);
    builder.setCancelable(false);

    switch (id) {
      case DIALOG_CONFIRM_DELETE_ID:
        builder
          .setTitle("Confirm Delete")
          .setMessage("Really delete?").setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              deleteSelectedTiles();
            }
          }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
          });
        return builder.create();

      case DIALOG_CONFIRM_NEXTSTEPS_ID:
        builder
          .setTitle("Version Problem")
          .setMessage("The base version for tiles has changed. What to do?")
          .setPositiveButton("Continue with current download, delete other old data", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

              ArrayList<Integer> allTiles = mBInstallerView.getSelectedTiles(MASK_INSTALLED_RD5);
              for (Integer sel : allTiles) {
                if (!selectedTiles.contains(sel)) {
                  mBInstallerView.toggleTileStatus(sel, 0);
                  new File(mBaseDir, "brouter/segments4/" + baseNameForTile(sel) + ".rd5").delete();
                }
              }
              downloadSelectedTiles();
            }
          }).setNegativeButton("Select all for download and start", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              downloadInstalledTiles();
            }
          }).setNeutralButton("Cancel now, complete on an other day", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              File tmplookupFile = new File(mBaseDir, "brouter/profiles2/lookups.dat.tmp");
              tmplookupFile.delete();
              finish();
            }
          });
        return builder.create();

      case DIALOG_CONFIRM_GETDIFFS_ID:
        builder
          .setTitle("Version Differences")
          .setMessage("The base version for some tiles is different. What to do?")
          .setPositiveButton("Download all different tiles", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              downloadDiffVersionTiles();
            }
          }).setNegativeButton("Drop all different tiles", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              dropDiffVersionTiles();
            }
          }).setNeutralButton("Cancel now, complete on an other day", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              finish();
            }
          });
        return builder.create();

      default:
        return null;
    }
  }

  void showADialog(int id) {
    Dialog d = createADialog(id);
    if (d != null) d.show();
  }

  public void showConfirmDelete() {
    showADialog(DIALOG_CONFIRM_DELETE_ID);
  }

  public void showConfirmNextSteps() {
    showADialog(DIALOG_CONFIRM_NEXTSTEPS_ID);
  }

  private void showConfirmGetDiffs() {
    showADialog(DIALOG_CONFIRM_GETDIFFS_ID);
  }


  private void scanExistingFiles() {
    mBInstallerView.clearAllTilesStatus(MASK_CURRENT_RD5 | MASK_INSTALLED_RD5 | MASK_DELETED_RD5 | MASK_SELECTED_RD5);

    scanExistingFiles(new File(mBaseDir, "brouter/segments4"));

    File secondary = RoutingHelper.getSecondarySegmentDir(new File(mBaseDir, "brouter/segments4"));
    if (secondary != null) {
      scanExistingFiles(secondary);
    }
  }

  private void scanExistingFiles(File dir) {
    String[] fileNames = dir.list();
    if (fileNames == null) return;
    String suffix = ".rd5";
    for (String fileName : fileNames) {
      if (fileName.endsWith(suffix)) {
        String basename = fileName.substring(0, fileName.length() - suffix.length());
        int tileIndex = tileForBaseName(basename);
        mBInstallerView.setTileStatus(tileIndex, MASK_INSTALLED_RD5);

        long age = System.currentTimeMillis() - new File(dir, fileName).lastModified();
        if (age < 10800000) mBInstallerView.setTileStatus(tileIndex, MASK_CURRENT_RD5); // 3 hours
      }
    }
  }

  private void deleteSelectedTiles() {
    ArrayList<Integer> selectedTiles = mBInstallerView.getSelectedTiles(MASK_DELETED_RD5);
    for (int tileIndex : selectedTiles) {
      new File(mBaseDir, "brouter/segments4/" + baseNameForTile(tileIndex) + ".rd5").delete();
    }
    scanExistingFiles();
  }

  private void downloadSelectedTiles() {
    selectedTiles = mBInstallerView.getSelectedTiles(MASK_SELECTED_RD5);
    downloadAll(selectedTiles, DownloadWorker.VALUE_SEGMENT_PARTS);
  }

  private void downloadInstalledTiles() {
    ArrayList<Integer> selectedTiles = mBInstallerView.getSelectedTiles(MASK_INSTALLED_RD5);
    ArrayList<Integer> tmpSelectedTiles = mBInstallerView.getSelectedTiles(MASK_SELECTED_RD5);
    if (tmpSelectedTiles.size() > 0) {
      selectedTiles.addAll(tmpSelectedTiles);
    }
    downloadAll(selectedTiles, DownloadWorker.VALUE_SEGMENT_ALL);
  }

  private void downloadDiffVersionTiles() {
    downloadAll(new ArrayList<Integer>(), DownloadWorker.VALUE_SEGMENT_DIFFS);
  }

  private void dropDiffVersionTiles() {
    downloadAll(new ArrayList<Integer>(), DownloadWorker.VALUE_SEGMENT_DROPDIFFS);
  }

  private boolean isDownloadRunning(Class<?> serviceClass) {
    WorkManager instance = WorkManager.getInstance(getApplicationContext());

    ListenableFuture<List<WorkInfo>> statuses = instance.getWorkInfosForUniqueWork(DownloadWorker.WORKER_NAME);
    try {
      boolean running = false;
      List<WorkInfo> workInfoList = statuses.get();
      for (WorkInfo workInfo : workInfoList) {
        WorkInfo.State state = workInfo.getState();
        running = state == WorkInfo.State.RUNNING | state == WorkInfo.State.ENQUEUED;
      }
      return running;
    } catch (ExecutionException e) {
      e.printStackTrace();
      return false;
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    }
  }

  void stopDownload() {
    WorkManager workManager = WorkManager.getInstance(getApplicationContext());
    workManager.cancelAllWork();
  }

  private int tileForBaseName(String basename) {
    String uname = basename.toUpperCase(Locale.ROOT);
    int idx = uname.indexOf("_");
    if (idx < 0) return -1;
    String slon = uname.substring(0, idx);
    String slat = uname.substring(idx + 1);
    int ilon = slon.charAt(0) == 'W' ? -Integer.parseInt(slon.substring(1)) :
      (slon.charAt(0) == 'E' ? Integer.parseInt(slon.substring(1)) : -1);
    int ilat = slat.charAt(0) == 'S' ? -Integer.parseInt(slat.substring(1)) :
      (slat.charAt(0) == 'N' ? Integer.parseInt(slat.substring(1)) : -1);
    if (ilon < -180 || ilon >= 180 || ilon % 5 != 0) return -1;
    if (ilat < -90 || ilat >= 90 || ilat % 5 != 0) return -1;
    return (ilon + 180) / 5 + 72 * ((ilat + 90) / 5);
  }

  private String baseNameForTile(int tileIndex) {
    int lon = (tileIndex % 72) * 5 - 180;
    int lat = (tileIndex / 72) * 5 - 90;
    String slon = lon < 0 ? "W" + (-lon) : "E" + lon;
    String slat = lat < 0 ? "S" + (-lat) : "N" + lat;
    return slon + "_" + slat;
  }
}
