// $todo: review

'use strict';

var yass = {};

yass.inherits = function (child, parent) {
  child.prototype = Object.create(parent.prototype);
  child.prototype.constructor = child;
};
