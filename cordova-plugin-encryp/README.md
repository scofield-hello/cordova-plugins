#  cordova-plugin-encryp(加密插件)
该插件可以用于加密敏感字符串(MD5的方式)
##  如何使用
cordova plugin add D:\\cordova-plugin-encryp --variable SALT='指定盐值'

```javascript

var option = {
  frequency:30,//加密次数
  random:32, //是否随机生成需要加密的字符串的长度，和source参数不能一起使用，如果都传了以source为准
  source:'password'//如果不需要自动生成则传入你想要加密的内容,且不需要传random属性
};

cordova.plugins.MD5Encryptor.encrypt(option, function (encryption) {
  var srcText = encryption.source;
  var destText = encryption.dest;
  var frequency = encryption.frequency;
}, function (error) {  
  console.log(error);
});
```
