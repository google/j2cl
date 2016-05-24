/**
 * @fileoverview Native type with varargs.
 */

goog.module('test.foo.JsTypeVarargsTest_MyNativeJsTypeVarargsConstructor');

class JsTypeVarargsTest_MyNativeJsTypeVarargsConstructor {
  /**
   * @param {number} i
   * @param {Array<*>} args
   */
  constructor(i, args) {
    this.a = arguments[1];
    this.b = arguments.length;
  }
}

exports = JsTypeVarargsTest_MyNativeJsTypeVarargsConstructor;
