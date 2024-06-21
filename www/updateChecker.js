var exec = require('cordova/exec');

var UpdateChecker = {
  registerReloadCallback: function(successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'UpdateChecker', 'registerReloadCallback', []);
  },
  userDecision: function(decision, successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'UpdateChecker', 'userDecision', [decision]);
  }
};

module.exports = UpdateChecker;
