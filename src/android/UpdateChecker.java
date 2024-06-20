package com.example.updatechecker;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker extends CordovaPlugin {

  private static final int CHECK_INTERVAL_MS = 60000; // 60 seconds
  private Handler handler;
  private Runnable checkRunnable;
  private String updateCheckUrl;
  private static final String TAG = "UpdateChecker";


  @Override
  protected void pluginInitialize() {
    handler = new Handler(Looper.getMainLooper());
    checkRunnable = new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "Running update check");
        checkForUpdate();
        handler.postDelayed(this, CHECK_INTERVAL_MS);
      }
    };
  }

  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);
    Log.d(TAG, "App resumed, starting update checker");
    startUpdateChecker();
  }

  @Override
  public void onReset() {
    super.onReset();
    Log.d(TAG, "App resumed, starting update checker");
    startUpdateChecker();
  }

  @Override
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);
    Log.d(TAG, "App paused, stopping update checker");
    stopUpdateChecker();
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("setUpdateCheckUrl")) {
      setUpdateCheckUrl(args.getString(0), callbackContext);
      return true;
    }
    return false;
  }

  private void setUpdateCheckUrl(String url, CallbackContext callbackContext) {
    updateCheckUrl = url;
    Log.d(TAG, "Update check URL set to: " + url);
    callbackContext.success("URL set successfully");
    startUpdateChecker();
  }

  private void checkForUpdate() {
    if (updateCheckUrl == null || updateCheckUrl.isEmpty()) {
        // No URL set, nothing to do
        Log.w(TAG, "No URL set for update checking");
        return;
    }
    cordova.getThreadPool().execute(() -> {
      try {
        Log.d(TAG, "Checking for updates at URL: " + updateCheckUrl);
        URL url = new URL(updateCheckUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.connect();

        long lastModified = connection.getLastModified();
        Log.d(TAG, "Last modified time from server: " + lastModified);
        long storedTimestamp = Long
            .parseLong(cordova.getActivity().getPreferences(MODE_PRIVATE).getString("lastModified", "0"));
        Log.d(TAG, "Stored last modified time: " + storedTimestamp);

        if (lastModified > storedTimestamp) {
          cordova.getActivity().runOnUiThread(() -> {
            Log.d(TAG, "Update available, prompting user to reload");
            new AlertDialog.Builder(cordova.getActivity())
                .setTitle("Update Available")
                .setMessage("A new version is available. Do you want to reload?")
                .setPositiveButton("Reload", (dialog, which) -> {
                  cordova.getActivity().getPreferences(MODE_PRIVATE).edit()
                      .putString("lastModified", Long.toString(lastModified)).apply();
                  cordova.getActivity().recreate();
                })
                .setNegativeButton("Later", null)
                .show();
          });
        }
        else {
          Log.d(TAG, "No update available");
        }
      } catch (IOException e) {
        Log.e(TAG, "Error checking for updates: ", e);
      }
    });
  }

  private void startUpdateChecker() {
    handler.post(checkRunnable);
  }

  private void stopUpdateChecker() {
    handler.removeCallbacks(checkRunnable);
  }
}

