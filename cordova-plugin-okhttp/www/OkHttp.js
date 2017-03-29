var exec = require('cordova/exec');
var OkHttp = function(){};

OkHttp.prototype.post = function(arg0, success, error) {
    exec(success, error, "OkHttp", "post", [arg0]);
};



var okHttpUtil = new OkHttp();
module.exports = okHttpUtil;