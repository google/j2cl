/**
 * Impl hand rolled.
 */
goog.module('nativebootstrap.Equality$impl');


/**
 * Miscellaneous equality functions.
 */
class Equality {
  /**
   * Strict equality that combines undefined and null.
   *
   * @param {*} left
   * @param {*} right
   * @return {boolean}
   */
  static $same(left, right) {
    return left === right || (left == null && right == null);
  }

  /**
   * Strict inequality that combines undefined and null.
   *
   * @param {*} left
   * @param {*} right
   * @return {boolean}
   */
  static $notSame(left, right) {
    return left !== right && (left != null || right != null);
  }
};


/**
 * Exported class.
 */
exports = Equality;
