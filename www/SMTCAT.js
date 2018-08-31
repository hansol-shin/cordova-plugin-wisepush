var exec = require('cordova/exec');

function SMTCAT() {
    console.log("SMTCAT.js: is created");
}

SMTCAT.prototype.connect = function( ip, port, success, error ){
	exec(success, error, "SMTCAT", 'connect', [ip, port]);
}

SMTCAT.prototype.disconnect = function( success, error ){
	exec(success, error, "SMTCAT", 'disconnect', []);
}

// UNSUBSCRIBE FROM TOPIC //
SMTCAT.prototype.isConnected = function( success, error ){
	exec(success, error, "SMTCAT", 'isConnected', []);
}

SMTCAT.prototype.trade = function( type, amt, instalment, arr, success, error ){
	exec(success, error, "SMTCAT", 'trade', [type, amt, instalment, arr]);
}

SMTCAT.prototype.tradeCancel = function( type, amt, instalment, approvalNo, approvalDttm, success, error ){
	exec(success, error, "SMTCAT", 'tradeCancel', [ type, amt, instalment, approvalNo, approvalDttm ]);
}

SMTCAT.prototype.print = function( arr, success, error ){
	exec(success, error, "SMTCAT", 'print', [arr]);
}

SMTCAT.prototype.cancel = function( success, error ){
	exec(success, error, "SMTCAT", 'cancel', []);
}

SMTCAT.prototype.setOrderNo = function( no, success, error ){
	exec(success, error, "SMTCAT", 'setOrderNo', [no]);
}

var smtcat = new SMTCAT();
module.exports = smtcat;