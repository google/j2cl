goog.module('test.foo.JsPropertyTest_MyNativeJsTypeInterface');

class JsPropertyTest_MyNativeJsTypeInterface {};

/**
 * @param {number} bias
 * @return {number}
 * @public
 */
JsPropertyTest_MyNativeJsTypeInterface.prototype.sum = function sum(bias) {
  return this.x + bias;
};

exports = JsPropertyTest_MyNativeJsTypeInterface;
