'use strict';

var yass = {};

yass.inherits = function (child, parent) {
  child.prototype = Object.create(parent.prototype);
  child.prototype.constructor = child;
};

yass.service = function (id, implementation /* , interceptors... */) {
};

yass.proxy = function (session, id /* , interceptors... */) {
};

yass.rpc = function (result, callback) {
  callback(result);
};
