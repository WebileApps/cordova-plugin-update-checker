package com.example.updatechecker;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker extends CordovaPlugin {
  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (action.equals("checkForUpdate")) {
      String url = args.getString(0);
      this.checkForUpdate(url, callbackContext);
      return true;
    }
    return false;
  }

  private void checkForUpdate(String urlString, CallbackContext callbackContext) {
    cordova.getThreadPool().execute(() -> {
      try {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        connection.connect();

        long lastModified = connection.getLastModified();
        callbackContext.success(Long.toString(lastModified));
      } catch (IOException e) {
        callbackContext.error("Failed to check for update: " + e.getMessage());
      }
    });
  }
}
