goog.module('vmbootstrap.CastsModule');


var ClassCastException =
    goog.require('gen.java.lang.ClassCastExceptionModule').ClassCastException;
var Object = goog.require('gen.java.lang.CoreModule').Object;


class Casts {
  /**
   * @param {Object} instance
   * @param {boolean} condition
   * @return {Object}
   */
  static to(instance, condition) {
    if (CAST_CHECKS_ENABLED_ && !condition) {
      Casts.throwCastException();
    }
    return instance;
  }

  /**
   * Isolates the exception throw here so that calling functions that perform
   * casts can still be optimized by V8.
   */
  static throwCastException() {
    throw ClassCastException.$create();
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
exports.Casts = Casts;
