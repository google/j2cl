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

  /**
   * Create a function that applies the specified jsFunctionMethod on itself,
   * and copies {@code instance}' properties to itself.
   *
   * @param {Function} jsFunctionMethod
   * @param {T} instance
   * @param {Function} copyMethod
   * @return {Function | T}
   * @template T
   * @public
   */
  static $makeLambdaFunction(jsFunctionMethod, instance, copyMethod) {
    var lambda = function() {
      return jsFunctionMethod.apply(lambda, arguments);
    };
    copyMethod(instance, lambda);
    return lambda;
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {}
};


/**
 * Used to store qualifier that is potentially side effecting.
 * @public {*}
 */
Util.$q = null;


/**
 * Exported class.
 */
exports = Util;
