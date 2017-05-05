/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Asserts$impl');


let AssertionError = goog.forwardDeclare('java.lang.AssertionError$impl');
let Exceptions = goog.forwardDeclare('vmbootstrap.Exceptions$impl');


class Asserts {
  /**
   * @param {boolean} condition
   */
  static $assert(condition) {
    Asserts.$clinit();
    if (!condition) {
      throw Exceptions.toJs(AssertionError.$create__());
    }
  }

  /**
   * @param {boolean} condition
   * @param {*} message
   */
  static $assertWithMessage(condition, message) {
    Asserts.$clinit();
    if (!condition) {
      throw Exceptions.toJs(AssertionError.$create__java_lang_Object(message));
    }
  }

  /**
   * @return {boolean} whether assertions are enabled
   */
  static $enabled() {
    return ASSERTIONS_ENABLED_;
  }

  /**
  * Runs inline static field initializers.
  * @protected
  */
  static $clinit() {
    AssertionError = goog.module.get('java.lang.AssertionError$impl');
    Exceptions = goog.module.get('vmbootstrap.Exceptions$impl');
  }
};


/**
 * @define {boolean} Whether or not to honor assertions. The value is checked
 *         here in assert methods but also the transpiler emits references at
 *         the call site so that disabling assertions stops execution (and
 *         potential side effects) in the condition expression.
 * @private
 */
goog.define('ASSERTIONS_ENABLED_', true);


/**
 * Exported class.
 */
exports = Asserts;
