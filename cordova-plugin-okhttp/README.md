# cordova-plugin-okhttp(http插件)

该插件可以进行网络请求，例如文件上传等，依赖于OkGo开源库
## 如何使用


- post请求
 ```javascript
 var defer = $q.defer();
 var config = {
   url:'',//string 接口URL
	 data:{//普通参数
    key1:'',
		key2:'',
		....
	},
	files:{//文件URI		
    file1:['','',''],
		file2:['','']
	}
 };
 cordova.plugins.OkHttp.post(config,function (data) {
        defer.resolve(data);
 }, function (error) {
        defer.reject(error);
 });
 return defer.promise;
 
 ```
