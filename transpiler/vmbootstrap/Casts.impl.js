/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Casts$impl');


let ClassCastException =
    goog.forwardDeclare('gen.java.lang.ClassCastException$impl');
let Exceptions = goog.forwardDeclare('vmbootstrap.Exceptions$impl');


class Casts {
  /**
   * @param {*} instance
   * @param {boolean} condition
   * @return {*}
   */
  static check(instance, condition) {
    if (CAST_CHECKS_ENABLED_ && !condition) {
      Casts.throwCastException();
    }
    return instance;
  }

  /**
   * @param {*} instance
   * @param {*} castType
   * @return {*}
   */
  static to(instance, castType) {
    return Casts.toInternal(
        instance, /** @type {Function} */ (castType.$isInstance));
  }

  /**
   * @param {*} instance
   * @param {Function} castTypeIsInstance
   * @return {*}
   */
  static toInternal(instance, castTypeIsInstance) {
    if (!CAST_CHECKS_ENABLED_) {
      return instance;
    }
    if (instance == null) {
      return instance;
    }
    if (!castTypeIsInstance(instance)) {
      Casts.throwCastException();
    }
    return instance;
  }

  /**
   * Isolates the exception throw here so that calling functions that perform
   * casts can still be optimized by V8.
   */
  static throwCastException() {
    Casts.$clinit();
    throw Exceptions.unwrap(ClassCastException.$create());
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    ClassCastException =
        goog.module.get('gen.java.lang.ClassCastException$impl');
    Exceptions = goog.module.get('vmbootstrap.Exceptions$impl');
  }
};


/**
 * @define {boolean} Whether or not to check casts.
 * @private
 */
goog.define('CAST_CHECKS_ENABLED_', true);

/**
 * Exported class.
 */
exports = Casts;
