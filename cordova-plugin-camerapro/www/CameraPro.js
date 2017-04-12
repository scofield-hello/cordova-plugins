var exec = require('cordova/exec');
var CameraPro = function(){};

CameraPro.prototype.capture = function(arg0, success, error) {
    exec(success, error, "CameraPro", "capture", [arg0]);
};

var camera = new CameraPro();
module.exports = camera;