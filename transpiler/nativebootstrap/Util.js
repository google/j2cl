goog.module('nativebootstrap.UtilModule');


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
   * @idGenerator {stable}
   * @public
   */
  static $generateId(id) { return id; }

  /**
   * Returns whether the "from" class can be cast to the "to" class.
   *
   * Unlike instanceof, this function operates on classes instead of
   * instances.
   */
  static $canCastClass(fromClass, toClass) {
    return (fromClass != null &&
            (fromClass == toClass || fromClass.prototype instanceof toClass));
  }
};


/**
 * Exported class.
 */
exports.Util = Util;
