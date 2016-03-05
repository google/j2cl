/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Comparables$impl');


let Boolean = goog.forwardDeclare('gen.java.lang.Boolean$impl');
let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let Comparable = goog.forwardDeclare('gen.java.lang.Comparable$impl');
let Double = goog.forwardDeclare('gen.java.lang.Double$impl');
let String = goog.forwardDeclare('gen.java.lang.String$impl');
let $boolean = goog.forwardDeclare('vmbootstrap.primitives.$boolean$impl');
let $double = goog.forwardDeclare('vmbootstrap.primitives.$double$impl');


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
      return Double.m_compareTo__java_lang_Double__java_lang_Object(
          /**@type {number} */ (a), /**@type {number} */ (b));
    } else if (type == 'boolean') {
      return Boolean.m_compareTo__java_lang_Boolean__java_lang_Object(
          /**@type {boolean} */ (a), /**@type {boolean} */ (b));
    } else if (type == 'string') {
      return String.m_compareTo__java_lang_String__java_lang_Object(
          /**@type {string} */ (a), /**@type {string} */ (b));
    }
    return a.m_compareTo__java_lang_Object(b);
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    Boolean = goog.module.get('gen.java.lang.Boolean$impl');
    Class = goog.module.get('gen.java.lang.Class$impl');
    Comparable = goog.module.get('gen.java.lang.Comparable$impl');
    Double = goog.module.get('gen.java.lang.Double$impl');
    String = goog.module.get('gen.java.lang.String$impl');
    $boolean = goog.module.get('vmbootstrap.primitives.$boolean$impl');
    $double = goog.module.get('vmbootstrap.primitives.$double$impl');
  }
};


/**
 * Exported class.
 */
exports = Comparables;
