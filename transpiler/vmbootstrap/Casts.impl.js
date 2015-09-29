/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Casts$impl');


let ClassCastException =
    goog.forwardDeclare('gen.java.lang.ClassCastException$impl');


class Casts {
  /**
   * @param {*} instance
   * @param {boolean} condition
   * @return {*}
   * @nocollapse
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
   * @nocollapse
   */
  static to(instance, castType) {
    if (CAST_CHECKS_ENABLED_ && !castType.$isInstance(instance)) {
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
    throw ClassCastException.$create();
  }

  /**
   * Runs inline static field initializers.
   * @protected
   * @nocollapse
   */
  static $clinit() {
    ClassCastException =
        goog.module.get('gen.java.lang.ClassCastException$impl');
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
