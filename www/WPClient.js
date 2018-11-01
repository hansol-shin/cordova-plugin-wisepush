var exec = require('cordova/exec');

function WPClient() {
    console.log("WPClient.js: is created");
}

// VERIFY CONNECTION
WPClient.prototype.isConnected = function( success, error ){
	exec(success, error, "WPPlugin", 'isConnected', []);
}
// GET DEVICE ID
WPClient.prototype.getDeviceId = function( success, error ){
	exec(success, error, "WPPlugin", 'getDeviceId', []);
}
// GET CLIENT ID
WPClient.prototype.getClientId = function( success, error ){
	exec(success, error, "WPPlugin", 'getClientId', []);
}
// SUBSCRIBE TO TOPIC //
WPClient.prototype.subscribeToTopic = function( topics, success, error ){
	exec(success, error, "WPPlugin", 'subscribeToTopic', [topics]);
}
// UNSUBSCRIBE FROM TOPIC //
WPClient.prototype.unsubscribeFromTopic = function( topics, success, error ){
	exec(success, error, "WPPlugin", 'unsubscribeFromTopic', [topics]);
}
// GET PREFERENCES //
WPClient.prototype.getPreferences = function( success, error ){
	exec(success, error, "WPPlugin", 'getPreferences', []);
}
// SET PREFERENCES //
WPClient.prototype.setPreferences = function( sound, vibrate, snooze, success, error ){
	exec(success, error, "WPPlugin", 'setPreferences', [sound, vibrate, snooze]);
}
// NOTIFICATION CALLBACK //
WPClient.prototype.onNotification = function( callback, success, error ){
	WPClient.prototype.onNotificationReceived = callback;
	exec(success, error, "WPPlugin", 'registerNotification',[]);
}
// DEFAULT NOTIFICATION CALLBACK //
WPClient.prototype.onNotificationReceived = function(payload){
	console.log("Received push notification");
	console.log(payload);
}

var wpClient = new WPClient();
module.exports = wpClient;