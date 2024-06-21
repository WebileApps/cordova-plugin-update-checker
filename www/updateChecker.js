var exec = require('cordova/exec');

var UpdateChecker = {
  init: function(successCallback, errorCallback) {
    exec(successCallback, errorCallback, 'UpdateChecker', 'init', []);
  }
};

module.exports = UpdateChecker;
