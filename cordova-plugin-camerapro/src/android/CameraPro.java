package com.bolu.plugins.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;

import com.bolu.camera.library.activity.CameraControl;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by Nick on 2017/4/11.
 */
public class CameraPro extends CordovaPlugin {
  private static final String TAG = "CameraPro";

  private static final int REQUEST = 1002;

  private static String[] requiredPermissions = {
    Manifest.permission.CAMERA,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE
  };

  private CallbackContext callbackContext;
  private JSONObject config;
  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.callbackContext = callbackContext;
    if ("capture".equals(action)){
      config = args.getJSONObject(0);
      if (checkRequiredPermission()){
        capturePhoto(config);
      }else{
        cordova.requestPermissions(this, REQUEST, requiredPermissions);
      }
      return true;
    }
    return false;
  }

  private void capturePhoto(JSONObject args) throws JSONException{
    boolean useFrontCamera = args.optBoolean("front_camera", false);
    boolean useFaceDetection = args.optBoolean("face_detection", false);
    Intent intent = new Intent(cordova.getActivity(), CameraControl.class);
    intent.putExtra(CameraControl.RATIO, 1);//1
    intent.putExtra(CameraControl.HDR_MODE, 1);//1
    intent.putExtra(CameraControl.FLASH_MODE, 1);//1
    intent.putExtra(CameraControl.FOCUS_MODE, 0);//0
    intent.putExtra(CameraControl.QUALITY, 0);//0
    intent.putExtra(CameraControl.FRONT_CAMERA, useFrontCamera);//false
    intent.putExtra(CameraControl.FACE_DETECTION, useFaceDetection);//false
    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath();
    intent.putExtra(CameraControl.PATH, directory);
    cordova.startActivityForResult(this, intent,CameraControl.REQUEST_CODE);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);
    if (requestCode == CameraControl.REQUEST_CODE){
      switch (resultCode){
        case -1:
          try{
            String filename = intent.getStringExtra("name");
            String filepath = intent.getStringExtra("path");
            File file = new File(filepath);
            String uri = file.toURI().toString();
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("filename", filename);
            jsonObject.put("filepath", filepath);
            jsonObject.put("uri", uri);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
            callbackContext.sendPluginResult(pluginResult);
          } catch (Exception e){
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "相机调用出错.") ;
            callbackContext.sendPluginResult(pluginResult);
            LOG.e(TAG, "拍照返回错误:"+e.getMessage(), e);
          }
          break;
        case 0:
          PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "用户取消操作.") ;
          callbackContext.sendPluginResult(pluginResult);
          LOG.w(TAG, "用户取消拍照");
          break;
      }
    }
  }

  private boolean checkRequiredPermission(){
    return (cordova.hasPermission(requiredPermissions[0])
      && cordova.hasPermission(requiredPermissions[1])
      && cordova.hasPermission(requiredPermissions[2]));
  }

  @Override
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
    for (int grantResult : grantResults) {
      if (grantResult != PackageManager.PERMISSION_GRANTED){
        LOG.e(TAG, "请先授予应用所需权限");
        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "请先授予应用所需权限.");
        callbackContext.sendPluginResult(pluginResult);
        return;
      }
    }
    capturePhoto(config);
  }
}
