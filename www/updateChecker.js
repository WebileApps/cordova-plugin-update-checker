var exec = require('cordova/exec');

var UpdateChecker = {
    setUrlAndTimeout: function(url, timeout, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'UpdateChecker', 'setUrlAndTimeout', [url, timeout]);
    },
    registerReloadCallback: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'UpdateChecker', 'registerReloadCallback', []);
    }
};

module.exports = UpdateChecker;
