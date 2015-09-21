/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Comparables$impl');


let Comparable = goog.forwardDeclare('gen.java.lang.Comparable$impl');
let Booleans = goog.forwardDeclare('vmbootstrap.Booleans$impl');
let Numbers = goog.forwardDeclare('vmbootstrap.Numbers$impl');
let Strings = goog.forwardDeclare('vmbootstrap.Strings$impl');


/**
 * Provides devirtualized method implementations for Comparable.
 */
class Comparables {
  /**
   * @param {Comparable|string|number|boolean} a
   * @param {*} b
   * @return {number}
   * @public
   */
  static m_compareTo__java_lang_Comparable__java_lang_Object(a, b) {
    Comparables.$clinit();
    var type = typeof a;
    if (type == 'number') {
      return Numbers.m_compareTo__java_lang_Number__java_lang_Object(
        /**@type {number} */ (a), b);
    } else if (type == 'boolean') {
      return Booleans.m_compareTo__java_lang_Boolean__java_lang_Object(
        /**@type {boolean} */ (a), b);
    } else if (type == 'string') {
      return Strings.m_compareTo__java_lang_String__java_lang_Object(
        /**@type {string} */ (a), b);
    }
    return a.m_compareTo__java_lang_Object(b);
  }

  /**
   * Runs inline static field initializers.
   * @protected
   * @nocollapse
   */
  static $clinit() {
    Comparable = goog.module.get('gen.java.lang.Comparable$impl');
    Booleans = goog.module.get('vmbootstrap.Booleans$impl');
    Numbers = goog.module.get('vmbootstrap.Numbers$impl');
    Strings = goog.module.get('vmbootstrap.Strings$impl');
  }
};


/**
 * Exported class.
 */
exports = Comparables;
