var exec = require('cordova/exec');
var Encryptor = function(){};

Encryptor.prototype.encrypt = function(arg0, success, error) {
    exec(success, error, "MD5Encryptor", "encrypt", [arg0]);
};

var encryptor = new Encryptor();
module.exports = encryptor;