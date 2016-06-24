goog.module('com.google.test.NativeType');

var Inner = goog.require('com.google.test.Inner$impl');
var Outer = goog.require('com.google.j2cl.transpiler.integration.jsinnerclass.Outer$impl');

class NativeType {
  /**
   * @param {Outer} outer
   * @return {number}
   */
  static getB(outer) { return new Inner(outer).getB(); }
}

exports = NativeType;
