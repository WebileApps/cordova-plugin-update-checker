var exec = require('cordova/exec');

var UpdateChecker = {
    checkForUpdate: function (url, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'UpdateChecker', 'checkForUpdate', [url]);
    }
};

module.exports = UpdateChecker;

