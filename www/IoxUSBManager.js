var exec = require('cordova/exec');

exports.coolMethod = function (arg0, success, error) {
    exec(success, error, 'IoxUSBManager', 'coolMethod', [arg0]);
};

exports.setCallback = function (successCb) {
    exec(successCb, function (err) {
        console.log(err);
    }, 'IoxUSBManager', 'setCallback', []);
};
