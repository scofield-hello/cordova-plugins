# cordova-plugin-camerapro
 自定义相机插件，可支持人脸检测(需设备支持)，依赖CameraApplication
 
## 如何使用
 ```javascript
//第一个参数front_camera:true 表示用前置相机，false表示用后置相机
//第二个参数face_detection:true表示使用人脸检测，false表示不使用人脸检测，为true时必须检测到人脸后才能点击拍照，如果设备不支持人脸检测，则也能拍照
var options = {front_camera:true, face_detection:true};
cordova.plugins.CameraPro.capture(options,function (data) {
  console.log('拍照返回照片URI：' + data.uri);
}, function (error) {
  console.log(error);
});
 
```
 
