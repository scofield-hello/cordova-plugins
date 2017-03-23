# cordova-plugin-sound-recorder(录音插件)
通过使用该插件可以指定调用系统麦克风录音并可以指定时间长度

## 如何使用
```javascript
$scope.record = function () {
    var config = {duration:25};//duration单位为秒 必须大于等于 1
    cordova.plugins.SoundRecorder.record(config,function (data) {
      console.log('--------------------录音成功！文件保存位置---：' + data.uri);
    },function (error) {
      console.log('--------------------录音失败！---：' + error);
    });
  };
```  
  
