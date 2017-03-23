var exec = require('cordova/exec');
var SoundRecorder = function(){};

SoundRecorder.prototype.record = function(arg0, success, error) {
    exec(success, error, "SoundRecorder", "record", [arg0]);
};


var recorder = new SoundRecorder();
module.exports = recorder;