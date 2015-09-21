/**
 * Impl hand rolled.
 */
goog.module('nativebootstrap.Util$impl');


/**
 * Miscellaneous utility functions.
 */
class Util {
  /**
   * Generates and returns a unique id from a String.
   *
   * Closure compiler can optionally replace this function with
   * some other strategy.
   *
   * @param {string} id
   * @return {string}
   * @public
   */
  static $generateId(id) { return id; }

  /**
   * Returns whether the "from" class can be cast to the "to" class.
   *
   * Unlike instanceof, this function operates on classes instead of
   * instances.
   * @public
   */
  static $canCastClass(fromClass, toClass) {
    return (fromClass != null &&
            (fromClass == toClass || fromClass.prototype instanceof toClass));
  }
};


/**
 * Exported class.
 */
exports = Util;
