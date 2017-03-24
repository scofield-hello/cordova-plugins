var exec = require('cordova/exec');
var SoundRecorder = function(){};

SoundRecorder.prototype.countdownRecord = function(arg0, success, error) {
    exec(success, error, "SoundRecorder", "countdownRecord", [arg0]);
};

SoundRecorder.prototype.manualRecord = function(success, error) {
    exec(success, error, "SoundRecorder", "manualRecord", []);
};

SoundRecorder.prototype.manualStop = function(success, error) {
    exec(success, error, "SoundRecorder", "manualStop", []);
};

var recorder = new SoundRecorder();
module.exports = recorder;