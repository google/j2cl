/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Asserts$impl');


// let AssertionError =
//     goog.forwardDeclare('gen.java.lang.AssertionError$impl');


class Asserts {
  /**
   * @param {boolean} condition
   */
  static $assert(condition) {
    // Asserts.$clinit();
    if (!condition) {
      // TODO(rluble): replace this with AssertionError. We are throwing Error
      // here to make debugging possible. Current tooling does not recover a
      // stack trace if a custom object is thrown.
      throw Error('Assertion failed.');
    }
  }

  /**
   * @param {boolean} condition
   * @param {?string} message
   */
  static $assertWithMessage(condition, message) {
    // Asserts.$clinit();
    if (!condition) {
      // TODO(rluble): replace this with
      //    throw AssertionError.$create__java_lang_String(message);
      throw Error(message);
    }
  }

  /**
   * @return {boolean} whether assertions are enabled
   */
  static $enabled() {
    return ASSERTIONS_ENABLED_;
  }
//
//  /**
//   * Runs inline static field initializers.
//   * @protected
//   */
//  static $clinit() {
//    AssertionError = goog.module.get('gen.java.lang.AssertionError$impl');
//  }
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
exports = Asserts;
