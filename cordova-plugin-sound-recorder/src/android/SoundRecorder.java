package com.bolu.plugins.sound;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateFormat;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 声音录制插件,在Javascript中通过
 * cordova.plugins.SoundRecorder调用
 */
public class SoundRecorder extends CordovaPlugin {
  private static final String TAG = "SoundRecorder";

  private final static int REQUEST_COUNTDOWN = 1001;
  private final static int REQUEST_MANUAL= 1002;

  private final static int DEFAULT_DURATION = 10;//默认录制时间 单位秒
  private final static int MODE_COUNTDOWN = 0;
  private final static int MODE_MANUAL = 1;
  private final static String AUDIO_DIR = "AudioRecords";
  private final static String DATE_FORMAT = "yyyyMMddHHmmss";
  private final static String RAW_SUFFIX = ".raw";
  private final static String WAV_SUFFIX = ".wav";

  private JSONObject config;
  private CallbackContext callbackContext;

  private final int simpleRate = 8000;//采样率
  private final int chanel = AudioFormat.CHANNEL_IN_DEFAULT;//输入设备声道
  private final int encoding = AudioFormat.ENCODING_PCM_16BIT;//比特率
  private final int bufferSize = AudioRecord.getMinBufferSize(simpleRate, chanel, encoding);//缓冲区大小
  private AudioRecord audioRecord;
  private int duration = DEFAULT_DURATION;
  private boolean isRecording = false;
  private String wavFilename,rawFilename;
  private int mode = MODE_COUNTDOWN;
  private final static String[] requiredPermissions = new String[]{
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE
  };

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.callbackContext = callbackContext;
    boolean actionResult = false;
    if (action.equals("countdownRecord")) {
      actionResult = true;
      config = args.getJSONObject(0);
      if (checkRequiredPermission()){
        this.countdownRecord(config, callbackContext);
      }else{
          mode = MODE_COUNTDOWN;
          cordova.requestPermissions(this, REQUEST_COUNTDOWN, requiredPermissions);
      }
      return actionResult;
    }
    if (action.equals("manualRecord")){
      actionResult = true;
      if (checkRequiredPermission()){
        this.manualRecord(callbackContext);
      }else{
        mode = MODE_MANUAL;
        cordova.requestPermissions(this, REQUEST_MANUAL, requiredPermissions);
      }
      return actionResult;
    }
    if (action.equals("manualStop")){
      actionResult = true;
      this.manualStop(callbackContext);
    }
    return actionResult;
  }

  private boolean checkRequiredPermission(){
    return (cordova.hasPermission(requiredPermissions[0])
      && cordova.hasPermission(requiredPermissions[1])
      && cordova.hasPermission(requiredPermissions[2]));
  }


  /**
   * 生成文件存放目录，初始化文件路径
   * @return true: 初始化成功，false: 初始化失败
   */
  private boolean initAudioFilenames(){
    String filename = DateFormat.format(DATE_FORMAT, System.currentTimeMillis()).toString();
    String directoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + AUDIO_DIR;
    File directory = new File(directoryPath);
    if (!directory.exists()) {
      if (!directory.mkdirs()) {
        LOG.e(TAG, "文件夹创建失败");
        return false;
      }
    }
    rawFilename = directoryPath + File.separator + filename + RAW_SUFFIX;
    wavFilename = directoryPath + File.separator + filename + WAV_SUFFIX;
    return true;
  }


  private boolean startRecording(){
    if (!isRecording){
      int audioSource = MediaRecorder.AudioSource.MIC;
      audioRecord = new AudioRecord(audioSource, simpleRate, chanel, encoding, bufferSize);
      audioRecord.startRecording();
      isRecording = true;
      return true;
    }
    return false;
  }

  /**
   * 实时处理音频文件，从RAW格式转换为WAV格式
   */
  private void writeWavFileInRealtime(){
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        LOG.d(TAG, "录音程序正在运行中....");
        byte[] soundBytes = new byte[bufferSize];
        FileOutputStream fileOutputStream;
        int read;
        File rawAudioFile = new File(rawFilename);
        if (rawAudioFile.exists()) {
          if (!rawAudioFile.delete()) {
            stopRecord();
            LOG.e(TAG, "处理音频文件出错,旧文件无法删除");
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "处理音频文件出错,旧文件找不到.");
            callbackContext.sendPluginResult(pluginResult);
            return;
          }
        }
        try {
          fileOutputStream = new FileOutputStream(rawAudioFile);
        } catch (FileNotFoundException e) {
          stopRecord();
          LOG.e(TAG, "处理音频文件出错,文件找不到", e);
          PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "处理音频文件出错,文件找不到.");
          callbackContext.sendPluginResult(pluginResult);
          return;
        }
        while (isRecording) {
          read = audioRecord.read(soundBytes, 0, bufferSize);
          if (AudioRecord.ERROR_INVALID_OPERATION != read) {
            try {
              fileOutputStream.write(soundBytes);
            } catch (IOException e) {
              stopRecord();
              LOG.e(TAG, "处理音频文件出错,写入时异常", e);
              PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "写入音频文件异常.");
              callbackContext.sendPluginResult(pluginResult);
              return;
            }
          }
        }
        try {
          fileOutputStream.close();
        } catch (IOException e) {
          stopRecord();
          LOG.e(TAG, "处理音频文件出错,关闭输出流时异常", e);
          PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "输出流关闭异常.");
          callbackContext.sendPluginResult(pluginResult);
          return;
        }
        long byteRate = 16 * simpleRate * chanel / 8;
        byte[] destData = new byte[bufferSize];
        try {
          FileInputStream fileInputStream = new FileInputStream(rawAudioFile);
          fileOutputStream = new FileOutputStream(wavFilename);
          long srcLength = fileInputStream.getChannel().size();
          long destLength = srcLength + 36;
          writeHeaderData(fileOutputStream, srcLength, destLength, simpleRate, chanel, byteRate);
          while (fileInputStream.read(destData) != -1) {
            fileOutputStream.write(destData);
          }
          fileInputStream.close();
          fileOutputStream.close();
        } catch (IOException e) {
          stopRecord();
          LOG.e(TAG, "处理音频文件出错,格式转换时异常", e);
          PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "转换WAV格式异常.");
          callbackContext.sendPluginResult(pluginResult);
        }
      }
    });
  }

  /**
   * 倒计时录音
   * @param config 配置信息
   * @param callbackContext 回调
   * @throws JSONException JSON异常
   */
  private void countdownRecord(JSONObject config, final CallbackContext callbackContext) throws  JSONException {
    mode = MODE_COUNTDOWN;
    duration = (config.has("duration") ? config.getInt("duration") : DEFAULT_DURATION);
    if (duration < 1) {
      PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "参数配置错误,录音时间最短为1秒.");
      callbackContext.sendPluginResult(pluginResult);
    } else {
      if (!initAudioFilenames()){
        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "初始化文件地址失败.");
        callbackContext.sendPluginResult(pluginResult);
        return;
      }
      if (!startRecording()){
        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "录音机实例初始化失败或已在录音中.");
        callbackContext.sendPluginResult(pluginResult);
        return;
      }
      monitorCountdownRecord();
      writeWavFileInRealtime();
    }
  }

  /**
   * 手动开始录音
   * @param callbackContext 回调
   */
  private void manualRecord(final CallbackContext callbackContext){
    duration = 0;
    mode = MODE_MANUAL;
    if (!initAudioFilenames()){
      PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "初始化文件地址失败.");
      callbackContext.sendPluginResult(pluginResult);
      return;
    }
    if (!startRecording()){
      PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "录音机实例初始化失败或已在录音中.");
      callbackContext.sendPluginResult(pluginResult);
    }else{
      PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
      result.setKeepCallback(true);// 保证持久回调
      callbackContext.sendPluginResult(result);
      monitorManualRecord();
      writeWavFileInRealtime();
    }
  }

  /**
   * 手动停止录音
   * @param callbackContext 回调
   * @throws JSONException JSON异常
   */
  private void manualStop(final CallbackContext callbackContext) throws JSONException{
    if (isRecording){
      stopRecord();
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("uri", Uri.fromFile(new File(wavFilename)));
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
      callbackContext.sendPluginResult(pluginResult);
      File originalRawFile = new File(rawFilename);
      if (originalRawFile.exists()){
        boolean result = originalRawFile.delete();
        LOG.d(TAG, "原始RAW录音文件删除" + (result ? "成功":"失败"));
      }
    }else{
      PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "录音已经停止.");
      callbackContext.sendPluginResult(pluginResult);
    }
  }


  private void monitorManualRecord(){
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
       while (isRecording){
         try {
           Thread.sleep(1000);
           duration ++;
           if (isRecording){
             LOG.d(TAG, "已录音时长" + duration+ "秒");
             PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, duration);
             pluginResult.setKeepCallback(true);
             callbackContext.sendPluginResult(pluginResult);
           }
         } catch (InterruptedException e) {
           stopRecord();
           LOG.e(TAG, "录音过程中出现线程异常", e);
           PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "录音过程中出现线程异常.");
           callbackContext.sendPluginResult(pluginResult);
         }
       }
      }
    });
  }

  private void monitorCountdownRecord(){
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        while (isRecording){
          try {
            Thread.sleep(1000);
            LOG.d(TAG, "录音剩余" + duration+ "秒");
            duration --;
            if (duration == 0){
              stopRecord();
              JSONObject jsonObject = new JSONObject();
              jsonObject.put("uri", Uri.fromFile(new File(wavFilename)));
              PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
              callbackContext.sendPluginResult(pluginResult);
              File originalRawFile = new File(rawFilename);
              if (originalRawFile.exists()){
                boolean result = originalRawFile.delete();
                LOG.d(TAG, "原始RAW录音文件删除" + (result ? "成功":"失败"));
              }
            }
          } catch (Exception e) {
            stopRecord();
            LOG.e(TAG, "录音过程中出现线程异常问题", e);
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, "录音过程中出现线程异常问题.");
            callbackContext.sendPluginResult(pluginResult);
          }
        }
      }
    });
  }

  private void stopRecord(){
      isRecording = false;
      audioRecord.stop();
      audioRecord.release();
      audioRecord = null;
      duration = 0;
      LOG.d(TAG, "录音程序已停止.");
  }

  private void writeHeaderData(FileOutputStream fileOutputStream, long srcLength,long destLength, long sampleRate, int channel, long byteRate) throws IOException{
    byte[] header = new byte[44];
    header[0] = 'R'; // RIFF/WAVE header
    header[1] = 'I';
    header[2] = 'F';
    header[3] = 'F';
    header[4] = (byte) (destLength & 0xff);
    header[5] = (byte) ((destLength >> 8) & 0xff);
    header[6] = (byte) ((destLength >> 16) & 0xff);
    header[7] = (byte) ((destLength >> 24) & 0xff);
    header[8] = 'W';
    header[9] = 'A';
    header[10] = 'V';
    header[11] = 'E';
    header[12] = 'f'; // 'fmt ' chunk
    header[13] = 'm';
    header[14] = 't';
    header[15] = ' ';
    header[16] = 16; // 4 bytes: size of 'fmt ' chunk
    header[17] = 0;
    header[18] = 0;
    header[19] = 0;
    header[20] = 1; // format = 1
    header[21] = 0;
    header[22] = (byte) channel;
    header[23] = 0;
    header[24] = (byte) (sampleRate & 0xff);
    header[25] = (byte) ((sampleRate >> 8) & 0xff);
    header[26] = (byte) ((sampleRate >> 16) & 0xff);
    header[27] = (byte) ((sampleRate >> 24) & 0xff);
    header[28] = (byte) (byteRate & 0xff);
    header[29] = (byte) ((byteRate >> 8) & 0xff);
    header[30] = (byte) ((byteRate >> 16) & 0xff);
    header[31] = (byte) ((byteRate >> 24) & 0xff);
    header[32] = (byte) (2 * 16 / 8); // block align
    header[33] = 0;
    header[34] = 16; // bits per sample
    header[35] = 0;
    header[36] = 'd';
    header[37] = 'a';
    header[38] = 't';
    header[39] = 'a';
    header[40] = (byte) (srcLength & 0xff);
    header[41] = (byte) ((srcLength >> 8) & 0xff);
    header[42] = (byte) ((srcLength >> 16) & 0xff);
    header[43] = (byte) ((srcLength >> 24) & 0xff);
    fileOutputStream.write(header, 0, 44);
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
    switch (mode){
      case MODE_COUNTDOWN:
        countdownRecord(config, callbackContext);
        break;
      case MODE_MANUAL:
        manualRecord(callbackContext);
        break;
      default:
        break;
    }
  }
}
