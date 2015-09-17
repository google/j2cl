goog.module('vmbootstrap.ComparablesModule');


let Comparable = goog.require('gen.java.lang.ComparableModule').Comparable;
let Booleans = goog.require('vmbootstrap.BooleansModule').Booleans;
let Numbers = goog.require('vmbootstrap.NumbersModule').Numbers;
let Strings = goog.require('vmbootstrap.StringsModule').Strings;

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
};


/**
 * Exported class.
 */
exports.Comparables = Comparables;
