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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker extends CordovaPlugin {

  private long lastModified = 0;
  private String updateCheckUrl;
  private static final String TAG = "UpdateChecker";
  private CallbackContext reloadCallbackContext;

  @Override
  protected void pluginInitialize() {
    Log.d(TAG, "Plugin initialized");
    updateCheckUrl = getContentUrlFromConfig();
    if (updateCheckUrl != null && !updateCheckUrl.isEmpty()) {
      Log.d(TAG, "Update check URL set to: " + updateCheckUrl);
      checkForUpdate();
    } else {
      Log.w(TAG, "No URL set for update checking");
    }
  }

  @Override
  public void onResume(boolean multitasking) {
    super.onResume(multitasking);
    Log.d(TAG, "App resumed, starting update checker");
    checkForUpdate();
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("userDecision")) {
      handleUserDecision(args.getString(0), callbackContext);
      return true;
    } else if (action.equals("registerReloadCallback")) {
      reloadCallbackContext = callbackContext;
      return true;
    }
    return false;
  }

  private void handleUserDecision(String decision, CallbackContext callbackContext) {
    if ("reload".equals(decision)) {
      cordova.getActivity().getPreferences(MODE_PRIVATE).edit()
              .putString("lastModified", Long.toString(lastModified)).apply();
      Log.d(TAG, "Timestamp updated");
      callbackContext.success("Timestamp updated");
    } else {
      Log.d(TAG, "Reload cancelled");
      callbackContext.success("Reload cancelled");
    }
  }

  private String getContentUrlFromConfig() {
    String url = null;
    try {
      InputStream inputStream = cordova.getActivity().getAssets().open("www/config.xml");
      XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
      XmlPullParser parser = factory.newPullParser();
      parser.setInput(inputStream, "UTF-8");

      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG && "content".equals(parser.getName())) {
          url = parser.getAttributeValue(null, "src");
          break;
        }
        eventType = parser.next();
      }
      inputStream.close();
    } catch (XmlPullParserException | IOException e) {
      Log.e(TAG, "Error reading config.xml", e);
    }
    return url;
  }

  private void checkForUpdate() {
    if (updateCheckUrl == null || updateCheckUrl.isEmpty()) {
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
}
