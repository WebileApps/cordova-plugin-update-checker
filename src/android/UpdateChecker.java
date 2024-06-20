package com.webile.updatechecker;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker extends CordovaPlugin {

  private static int CHECK_INTERVAL_MS = -1;
  private long lastModified = 0;
  private Handler handler;
  private Runnable checkRunnable;
  private String updateCheckUrl;
  private static final String TAG = "UpdateChecker";
  private CallbackContext reloadCallbackContext;

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
  public void onPause(boolean multitasking) {
    super.onPause(multitasking);
    Log.d(TAG, "App paused, stopping update checker");
    stopUpdateChecker();
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("setUrlAndTimeout")) {
      setUrlAndTimeout(args.getString(0), args.getInt(1), callbackContext);
      return true;
    } else if (action.equals("registerReloadCallback")) {
      reloadCallbackContext = callbackContext;
      return true;
    } else if (action.equals("userDecision")) {
      handleUserDecision(args.getString(0), callbackContext);
      return true;
    }
    return false;
  }
  private void handleUserDecision(String decision, CallbackContext callbackContext) {
    if ("reload".equals(decision)) {
      cordova.getActivity().getPreferences(MODE_PRIVATE).edit()
              .putString("lastModified", Long.toString(lastModified)).apply();
      callbackContext.success("Timestamp updated");
    } else {
      callbackContext.success("Reload cancelled");
    }
  }

  private void setUrlAndTimeout(String url, int timeOut, CallbackContext callbackContext) {
    if (CHECK_INTERVAL_MS == -1) {
      CHECK_INTERVAL_MS = timeOut;
      Log.d(TAG, "Timeout set to: " + timeOut);
    }
    if (updateCheckUrl == null) {
      updateCheckUrl = url;
      Log.d(TAG, "Update check URL set to: " + url);
      callbackContext.success("URL set successfully");
      startUpdateChecker();
    }
  }

  private void checkForUpdate() {
    if (updateCheckUrl == null || updateCheckUrl.isEmpty()) {
      Log.w(TAG, "No URL set for update checking");
      return;
    }
    if (CHECK_INTERVAL_MS == -1) {
      Log.w(TAG, "Timeout not set");
      return;
    }
    cordova.getThreadPool().execute(() -> {
      try {
        Log.d(TAG, "Checking for updates at URL: " + updateCheckUrl);
        URL url = new URL(updateCheckUrl);
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
            if (reloadCallbackContext != null) {
              PluginResult result = new PluginResult(PluginResult.Status.OK, "prompt_reload");
              result.setKeepCallback(true);
              reloadCallbackContext.sendPluginResult(result);
            }
          });
        } else {
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
