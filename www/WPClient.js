var exec = require('cordova/exec');

function WPClient() {
    console.log("WPClient.js: is created");
}

// GET DEVICE ID
FCMPlugin.prototype.getDeviceId = function( success, error ){
	exec(success, error, "WPClient", 'getDeviceId', []);
}
// GET CLIENT ID
FCMPlugin.prototype.getClientId = function( success, error ){
	exec(success, error, "WPClient", 'getClientId', []);
}
// SUBSCRIBE TO TOPIC //
FCMPlugin.prototype.subscribeToTopic = function( topic, success, error ){
	exec(success, error, "WPClient", 'subscribeToTopic', [topic]);
}
// UNSUBSCRIBE FROM TOPIC //
FCMPlugin.prototype.unsubscribeFromTopic = function( topic, success, error ){
	exec(success, error, "WPClient", 'unsubscribeFromTopic', [topic]);
}
// NOTIFICATION CALLBACK //
CMPlugin.prototype.onNotification = function( callback, success, error ){
	FCMPlugin.prototype.onNotificationReceived = callback;
	exec(success, error, "WPClient", 'registerNotification',[]);
}
// DEVICE ID REFRESH CALLBACK //
FCMPlugin.prototype.onDeviceIdChange = function( callback ){
	FCMPlugin.prototype.onDeviceIdChanged = callback;
}
// DEFAULT NOTIFICATION CALLBACK //
WPClient.prototype.onNotificationReceived = function(payload){
	console.log("Received push notification")
	console.log(payload)
}
// DEFAULT DEVICE ID REFRESH CALLBACK //
FCMPlugin.prototype.onDeviceIdChanged = function(deviceId){
	console.log("Device ID refresh")
	console.log(deviceId)
}

var wpClient = new WPClient();
module.exports = wpClient;