package com.example.updatechecker;

import android.os.Handler;
import android.os.Looper;

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

  @Override
  protected void pluginInitialize() {
    handler = new Handler(Looper.getMainLooper());
    checkRunnable = new Runnable() {
      @Override
      public void run() {
        checkForUpdate();
        handler.postDelayed(this, CHECK_INTERVAL_MS);
      }
    };
  }

  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);
    startUpdateChecker();
  }

  @Override
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);
    stopUpdateChecker();
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("checkForUpdate")) {
      checkForUpdate(callbackContext);
      return true;
    }
    if (action.equals("setUpdateCheckUrl")) {
      setUpdateCheckUrl(args.getString(0), callbackContext);
      return true;
    }
    return false;
  }

  private void setUpdateCheckUrl(String url, CallbackContext callbackContext) {
    updateCheckUrl = url;
    callbackContext.success("URL set successfully");
  }

  private void checkForUpdate(CallbackContext callbackContext) {
    cordova.getThreadPool().execute(() -> {
      try {
        String urlString = args.getString(0); // The URL should be passed as an argument
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.connect();

        long lastModified = connection.getLastModified();
        callbackContext.success(Long.toString(lastModified));
      } catch (IOException e) {
        callbackContext.error("Failed to check for update: " + e.getMessage());
      } catch (JSONException e) {
        callbackContext.error("Invalid URL parameter: " + e.getMessage());
      }
    });
  }

  private void checkForUpdate() {
    if (updateCheckUrl == null || updateCheckUrl.isEmpty()) {
        // No URL set, nothing to do
        return;
    }
    cordova.getThreadPool().execute(() -> {
      try {
        URL url = new URL(updateCheckUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.connect();

        long lastModified = connection.getLastModified();
        long storedTimestamp = Long
            .parseLong(cordova.getActivity().getPreferences(MODE_PRIVATE).getString("lastModified", "0"));

        if (lastModified > storedTimestamp) {
          cordova.getActivity().runOnUiThread(() -> {
            // Use WebView or any other method to display the update dialog
            // For example, show an AlertDialog
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
      } catch (IOException e) {
        // Handle the error
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
