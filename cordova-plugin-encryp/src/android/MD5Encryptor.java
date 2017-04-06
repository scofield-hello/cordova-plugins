package com.bolu.plugins.encryp;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

/**
 * Created by Nick on 2017/4/1.
 */
public class MD5Encryptor extends CordovaPlugin{

  private static final String TAG = "MD5Encryptor";

  private static final String BASE_STRING = "abcdefghijklmnopqrstuvwxyz0123456789";

  private static final String META_KEY = "com.bolu.plugins.encryp.SALT";

  private static MD5Encoder encoder;
  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    String salt = getMetaDataStringApplication(META_KEY, "SALT");
    encoder = new MD5Encoder(salt);
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if ("encrypt".equalsIgnoreCase(action)) {
      JSONObject arg = args.getJSONObject(0);
      encrypt(arg, callbackContext);
      return true;
    }
    return false;
  }


  private void encrypt(JSONObject args, CallbackContext callbackContext) throws JSONException{
    int frequency = args.optInt("frequency", 1);
    int random = args.optInt("random", 0);
    String source = args.optString("source");
    if (frequency < 1){
      PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "frequency 参数传递错误.");
      callbackContext.sendPluginResult(pluginResult);
      return;
    }
    if (random <= 0 && "".equals(source)){
      PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "random 或source 参数传递错误.");
      callbackContext.sendPluginResult(pluginResult);
      return;
    }
    if (!"".equals(source)){
      String encrypted = encoder.encrypt(source, frequency);
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("source", source);
      jsonObject.put("dest", encrypted);
      jsonObject.put("frequency", frequency);
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
      callbackContext.sendPluginResult(pluginResult);
      return;
    }
    if (random > 0){
      String rawRandom = newRandom(random);
      String encrypted = encoder.encrypt(rawRandom, frequency);
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("source", rawRandom);
      jsonObject.put("dest", encrypted);
      jsonObject.put("frequency", frequency);
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
      callbackContext.sendPluginResult(pluginResult);
    }
  }

  private String newRandom(int length) {
    Random random = new Random();
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < length; i++) {
      int number = random.nextInt(BASE_STRING.length());
      sb.append(BASE_STRING.charAt(number));
    }
    return sb.toString();
  }

  private Bundle getAppMetaDataBundle(PackageManager packageManager,
                                      String packageName) {
    Bundle bundle = null;
    try {
      ApplicationInfo ai = packageManager.getApplicationInfo(packageName,
        PackageManager.GET_META_DATA);
      bundle = ai.metaData;
    } catch (PackageManager.NameNotFoundException e) {
      LOG.e(TAG, e.getMessage(), e);
    }
    return bundle;
  }

  private String getMetaDataStringApplication(String key, String defValue) {
    Bundle bundle = getAppMetaDataBundle(cordova.getActivity().getPackageManager(), cordova.getActivity().getPackageName());
    if (bundle != null && bundle.containsKey(key)) {
      return bundle.getString(key);
    }
    return defValue;
  }
}
