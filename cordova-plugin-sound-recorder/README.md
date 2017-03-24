# cordova-plugin-sound-recorder(录音插件)

通过使用该插件可以指定调用系统麦克风录音并可以指定时间长度

## 如何使用

- 倒计时录音
 ```javascript
 var config = {duration:25};
 cordova.plugins.SoundRecorder.countdownRecord(config,function (data) {
  console.log('--------------------录音成功！---：' + data.uri);
 },function (error) {  
  console.log('--------------------录音失败！---：' + error);
 });
 
 ```

- 手动开始/结束录音
```javascript
//手动开始
$scope.manualStart = function () {  
 cordova.plugins.SoundRecorder.manualRecord(function (seconds) {    
  console.log('已录音' + seconds+"秒");
 },function (message) {    
  console.log(message);  
 }
)};

//手动停止
$scope.manualEnd = function () {  
 cordova.plugins.SoundRecorder.manualStop(function (data) {
  console.log('--------------------录音成功！---：' + data.uri);  
 },function (message) {   
  console.log(message);  
 }
)};
```
