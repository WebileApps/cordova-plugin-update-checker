package com.webile.updatechecker;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.ConfigXmlParser;
import org.apache.cordova.engine.SystemWebViewEngine;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker extends CordovaPlugin {

  private long lastModified = 0;
  private String launchUrl;
  private static final String TAG = "UpdateChecker";

  @Override
  protected void pluginInitialize() {
    Log.d(TAG, "Plugin initialized");
    launchUrl = getLaunchUrlFromConfig();
    if (launchUrl != null && !launchUrl.isEmpty()) {
      Log.d(TAG, "Update check URL set to: " + launchUrl);
      checkForUpdate();
    } else {
      Log.w(TAG, "No URL set for update checking");
    }
  }
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    Log.d(TAG, "Execute called with action: " + action);
    if (action.equals("init")) {
      callbackContext.success("UpdateChecker initialized");
      return true;
    }
    return false;
  }

  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);
    Log.d(TAG, "App resumed, starting update checker");
    checkForUpdate();
  }

  private String getLaunchUrlFromConfig() {
    ConfigXmlParser parser = new ConfigXmlParser();
    parser.parse(cordova.getActivity());
    return parser.getLaunchUrl();
  }

  private void checkForUpdate() {
    if (launchUrl == null || launchUrl.isEmpty()) {
      Log.w(TAG, "No URL set for update checking");
      return;
    }
    cordova.getThreadPool().execute(() -> {
      try {
        Log.d(TAG, "Checking for updates at URL: " + launchUrl);
        URL url = new URL(launchUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.connect();

        lastModified = connection.getLastModified();
        Log.d(TAG, "Last modified time from server: " + lastModified);
        long storedTimestamp = Long
                .parseLong(cordova.getActivity().getPreferences(MODE_PRIVATE).getString("lastModified", "0"));
        Log.d(TAG, "Stored last modified time: " + storedTimestamp);

        if (lastModified > storedTimestamp) {
          cordova.getActivity().runOnUiThread(() -> {
            Log.d(TAG, "Update available, prompting user to reload");
            showUpdateDialog();
          });
        } else {
          Log.d(TAG, "No update available");
        }
      } catch (IOException e) {
        Log.e(TAG, "Error checking for updates: ", e);
      }
    });
  }

  private void showUpdateDialog() {
    new AlertDialog.Builder(cordova.getActivity())
            .setTitle("Update Available")
            .setMessage("A new version is available. Do you want to reload?")
            .setPositiveButton("Reload", (dialog, which) -> {
              cordova.getActivity().getPreferences(MODE_PRIVATE).edit()
                      .putString("lastModified", Long.toString(lastModified)).apply();
              reloadWebView();
            })
            .setNegativeButton("Later", null)
            .show();
  }

  private void reloadWebView() {
    SystemWebViewEngine engine = (SystemWebViewEngine) this.webView.getEngine();
    engine.loadUrl(launchUrl, false);
  }
}
