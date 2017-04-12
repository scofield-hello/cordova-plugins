# cordova-plugin-camerapro
 自定义相机插件，可支持人脸检测(需设备支持)，依赖CameraApplication
 
## 如何使用
 ```javascript
var options = {front_camera:true, face_detection:true};
cordova.plugins.CameraPro.capture(options,function (data) {
  console.log('拍照返回照片URI：' + data.uri);
}, function (error) {
  console.log(error);
});
 
```
 
