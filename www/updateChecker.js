var exec = require('cordova/exec');

var UpdateChecker = {
    setUpdateCheckUrl: function(url, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'UpdateChecker', 'setUpdateCheckUrl', [url]);
    },
    registerReloadCallback: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'UpdateChecker', 'registerReloadCallback', []);
    }
};

module.exports = UpdateChecker;
