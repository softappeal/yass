var yass = {};

yass.inherits = function (child, parent) {
  'use strict';
  child.prototype = Object.create(parent.prototype);
  child.prototype.constructor = child;
};

yass.service = function (id, implementation /* , interceptors... */) {
  'use strict';
};

yass.proxy = function (session, id /* , interceptors... */) {
  'use strict';
};

yass.rpc = function (result, callback) {
  'use strict';
  callback(result);
};
