goog.module('vmbootstrap.AssertsModule');


var AssertionError =
    goog.require('gen.java.lang.AssertionErrorModule').AssertionError;

class Asserts {
  /**
   * @param {boolean} condition
   */
  static $assert(condition) {
    if (!condition) {
      throw AssertionError.$create();
    }
  }

  // TODO: uncomment this function once AssertionError.java is fully
  // implemented.
  /**
   * @param {boolean} condition
   * @param {?string} message
   */
  //static $assertWithMessage(condition, message) {
  //  if (!condition) {
  //    throw AssertionError.$create__java_lang_String(message);
  //  }
  //}

  /**
   * @return {boolean} whether assertions are enabled
   */
  static $enabled() {
    return ASSERTIONS_ENABLED_;
  }
};


/**
 * @define {boolean} Whether or not to honor assertions. The value is checked
 *         here in assert methods but also the transpiler emits references at
 *         the call site so that disabling assertions stops execution (and
 *         potential side effects) in the condition expression.
 * @private
 */
goog.define('ASSERTIONS_ENABLED_', false);


/**
 * Exported class.
 */
exports.Asserts = Asserts;
