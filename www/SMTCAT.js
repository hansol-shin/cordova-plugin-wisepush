var exec = require('cordova/exec');

function SMTCAT() {
    console.log("SMTCAT.js: is created");
}

SMTCAT.prototype.connect = function( ip, port, success, error ){
	exec(success, error, "SMTCAT", 'connect', [ip, port]);
}

SMTCAT.prototype.disconnect = function( ip, port, success, error ){
	exec(success, error, "SMTCAT", 'disconnect', [ip, port]);
}

// UNSUBSCRIBE FROM TOPIC //
SMTCAT.prototype.isConnected = function( success, error ){
	exec(success, error, "SMTCAT", 'isConnected', []);
}

SMTCAT.prototype.trade = function( type, amt, instalment, arr, success, error ){
	exec(success, error, "SMTCAT", 'trade', [type, amt, instalment, arr]);
}

SMTCAT.prototype.print = function( arr, success, error ){
	exec(success, error, "SMTCAT", 'print', [arr]);
}

SMTCAT.prototype.cancel = function( success, error ){
	exec(success, error, "SMTCAT", 'cancel', []);
}

var smtcat = new SMTCAT();
module.exports = smtcat;