goog.module('global.JsPropertyTest_MyNativeJsType');

class JsPropertyTest_MyNativeJsType {
  constructor(x) {
    this.x = x;
    this.y = undefined;
    this.ctorExecuted = true;
  }

  /**
   * @return {number}
   * @public
   * @nocollapse
   */
  static answerToLife() { return 42; }

  /**
   * @param {number} bias
   * @return {number}
   * @public
   */
  sum(bias) { return this.x + bias; }
};

/**
 * @public {number}
 */
JsPropertyTest_MyNativeJsType.staticX = 33;

exports = JsPropertyTest_MyNativeJsType;
