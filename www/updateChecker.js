var exec = require('cordova/exec');

var UpdateChecker = {
    setUpdateCheckUrl: function(url, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'UpdateChecker', 'setUpdateCheckUrl', [url]);
    }
};

module.exports = UpdateChecker;
