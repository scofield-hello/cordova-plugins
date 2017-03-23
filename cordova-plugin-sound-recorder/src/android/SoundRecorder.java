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

  private final static int PERMISSION_REQUEST_CODE = 1001;
  private final static int DEFAULT_DURATION = 10;//默认录制时间 单位秒
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
  private final static String[] requiredPermissions = new String[]{
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.WRITE_EXTERNAL_STORAGE,
    Manifest.permission.READ_EXTERNAL_STORAGE
  };

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.callbackContext = callbackContext;
    if (action.equals("record")) {
      config = args.getJSONObject(0);
      if (cordova.hasPermission(requiredPermissions[0])
        && cordova.hasPermission(requiredPermissions[1])
        && cordova.hasPermission(requiredPermissions[2])){
        this.startRecord(config, callbackContext);
      }else{
          cordova.requestPermissions(this,PERMISSION_REQUEST_CODE, requiredPermissions);
      }
      return true;
    }
    return false;
  }

  private void monitorAutoStop(final String rawFilename, final String wavFilename){
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
              callbackContext.success(jsonObject);
              File originalRawFile = new File(rawFilename);
              if (originalRawFile.exists()){
                boolean result = originalRawFile.delete();
                LOG.d(TAG, "原始RAW录音文件删除" + (result ? "成功":"失败"));
              }
            }
          } catch (Exception e) {
            stopRecord();
            LOG.e(TAG, "录音过程中出现线程异常问题", e);
            callbackContext.error("录音过程中出现线程异常问题");
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
    LOG.d(TAG, "录音程序已停止");
  }

  private void startRecord(JSONObject config, final CallbackContext callbackContext) throws  JSONException {
    duration = (config.has("duration") ? config.getInt("duration") : DEFAULT_DURATION);
    if (duration < 1) {
      callbackContext.error("参数配置错误,录音时间最短为1秒");
    } else {
      String filename = DateFormat.format(DATE_FORMAT, System.currentTimeMillis()).toString();
      String directoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + AUDIO_DIR;
      File directory = new File(directoryPath);
      if (! directory.exists()){
        if (!directory.mkdirs()){
          LOG.e(TAG, "文件夹创建失败");
          callbackContext.error("文件夹创建失败.");
          return;
        }
      }
      final String rawFilename = directoryPath + "/" + filename + RAW_SUFFIX;
      final String wavFilename = directoryPath + "/" + filename + WAV_SUFFIX;
      int audioSource = MediaRecorder.AudioSource.MIC;
      audioRecord = new AudioRecord(audioSource, simpleRate, chanel, encoding, bufferSize);
      audioRecord.startRecording();
      isRecording = true;
      monitorAutoStop(rawFilename, wavFilename);
      cordova.getThreadPool().execute(new Runnable() {
        @Override
        public void run() {
          LOG.d(TAG, "录音程序正在运行中....");
          byte[] soundBytes = new byte[bufferSize];
          FileOutputStream fileOutputStream;
          int read;
          File rawAudioFile = new File(rawFilename);
          if (rawAudioFile.exists()){
            if (!rawAudioFile.delete()){
              stopRecord();
              LOG.e(TAG, "处理音频文件出错,旧文件无法删除");
              callbackContext.error("处理音频文件出错,旧文件找不到");
              return;
            }
          }
          try{
            fileOutputStream = new FileOutputStream(rawAudioFile);
          } catch (FileNotFoundException e){
            stopRecord();
            LOG.e(TAG, "处理音频文件出错,文件找不到", e);
            callbackContext.error("处理音频文件出错,文件找不到");
            return;
          }
          while(isRecording){
            read = audioRecord.read(soundBytes, 0, bufferSize);
            if (AudioRecord.ERROR_INVALID_OPERATION != read){
              try {
                fileOutputStream.write(soundBytes);
              } catch (IOException e) {
                stopRecord();
                LOG.e(TAG, "处理音频文件出错,写入时异常", e);
                callbackContext.error("处理音频文件出错,写入时异常");
                return;
              }
            }
          }
          try {
            fileOutputStream.close();
          } catch (IOException e) {
            stopRecord();
            LOG.e(TAG, "处理音频文件出错,关闭输出流时异常", e);
            callbackContext.error("处理音频文件出错,关闭输出流时异常");
            return;
          }
          long byteRate = 16 * simpleRate * chanel /8;
          byte[] destData = new byte[bufferSize];
          try{
            FileInputStream fileInputStream = new FileInputStream(rawAudioFile);
            fileOutputStream = new FileOutputStream(wavFilename);
            long srcLength = fileInputStream.getChannel().size();
            long destLength = srcLength + 36;
            writeHeaderData(fileOutputStream, srcLength, destLength, simpleRate, chanel, byteRate);
            while(fileInputStream.read(destData) != -1){
              fileOutputStream.write(destData);
            }
            fileInputStream.close();
            fileOutputStream.close();
          }catch (IOException e){
            stopRecord();
            LOG.e(TAG, "处理音频文件出错,格式转换时异常", e);
            callbackContext.error("处理音频文件出错,格式转换时异常");
          }
        }
      });
    }
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
        callbackContext.error("请先授予应用所需权限.");
        return;
      }
    }
    startRecord(config,callbackContext);
  }
}
