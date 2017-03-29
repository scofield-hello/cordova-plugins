package com.bolu.plugins.okhttp;

import android.net.Uri;

import com.bolu.cordova.security.PwdEncoder;
import com.bolu.cordova.security.SecurityEncoder;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.HttpParams;
import com.lzy.okgo.request.PostRequest;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;

import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by Nick on 2017/3/27.
 */
public class OkHttp extends CordovaPlugin{
  private static final String TAG = "OkHttp";

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    OkGo.init(cordova.getActivity().getApplication());
    OkGo.getInstance().debug("TAG", Level.INFO, true).setRetryCount(0);
    LOG.d(TAG,"OkGo初始化");
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    JSONObject params = args.getJSONObject(0);
    if ("post".equals(action)){
      post(params, callbackContext);
      return true;
    }
    return false;
  }


  private void post(JSONObject params, final CallbackContext callbackContext) throws  JSONException{
    String url = params.optString("url");
    JSONObject data = params.optJSONObject("data");
    JSONObject files = params.optJSONObject("files");
    final CordovaResourceApi resourceApi = webView.getResourceApi();
    final Uri targetUri = resourceApi.remapUri(Uri.parse(url));
    int uriType = CordovaResourceApi.getUriType(targetUri);
    if (CordovaResourceApi.URI_TYPE_HTTP != uriType){
      callbackContext.error("URL参数有误");
      return;
    }
    HttpParams httpParams = new HttpParams();
    String token = SecurityEncoder.genToken(32);
    String signature = PwdEncoder.encrypt(token);
    httpParams.put("v_name",token);
    httpParams.put("v_value", signature);
    if (data != null){
      for (Iterator<?> iterator = data.keys(); iterator.hasNext();){
        String key = iterator.next().toString();
        String value = (data.opt(key) != null) ?  String.valueOf(data.opt(key)) : "";
        httpParams.put(key,value,true);
      }
    }
    if (files != null){
      for (Iterator<?> iterator = files.keys(); iterator.hasNext();){
        ArrayList<File> fileArrayList = new ArrayList<File>();
        String key = iterator.next().toString();
        JSONArray fileUris = files.optJSONArray(key);
        if (fileUris == null){
          continue;
        }
        int urisLength = fileUris.length();
        for (int i = 0; i < urisLength; i++){
          String fileUri = fileUris.optString(i, null);
          LOG.d(TAG, "文件路径：key:" +key+"路径"+ fileUri);
          if (fileUri == null || "".equals(fileUri)){
            continue;
          }
          try{
            URI uri = URI.create(fileUri);
            fileArrayList.add(new File(uri));
          } catch (IllegalArgumentException e){
            LOG.e(TAG,"文件路径参数传递有误",e);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "文件路径参数传递有误.");
            callbackContext.sendPluginResult(pluginResult);
            return;
          }
        }
        if (!fileArrayList.isEmpty()){
          httpParams.putFileParams(key,fileArrayList);
        }
      }
    }
    //发起请求
    PostRequest postRequest = OkGo.post(url).tag(TAG);
    postRequest.params(httpParams).execute(new StringCallback() {
      @Override
      public void onSuccess(String s, Call call, Response response){
        PluginResult pluginResult;
        try{
          JSONObject jsonObject  = new JSONObject(s);
          pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
        }catch (JSONException e){
          LOG.e(TAG, "String 转 JSON时出现异常", e);
          pluginResult = new PluginResult(PluginResult.Status.ERROR, "服务端返回的数据异常.");
        }
        callbackContext.sendPluginResult(pluginResult);
      }
      @Override
      public void onError(Call call, Response response, Exception e) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "请求时网络/服务出错.");
        callbackContext.sendPluginResult(pluginResult);
      }
    });
  }
}
