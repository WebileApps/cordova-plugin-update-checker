var exec = require('cordova/exec');

var UpdateChecker = {
    setUpdateCheckUrl: function(url, timeout, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'UpdateChecker', 'setUpdateCheckUrl', [url, timeout]);
    },
    registerReloadCallback: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'UpdateChecker', 'registerReloadCallback', []);
    }
};

module.exports = UpdateChecker;
