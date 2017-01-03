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
};

// Ensure Equality.$same() is not inlined so J2clEqualitySameRewriterPass can
// detect all calls.
// TODO(goktug): we should have a better way of doing this.
goog.global['_$same'] = Equality.$same;

/**
 * Exported class.
 */
exports = Equality;
