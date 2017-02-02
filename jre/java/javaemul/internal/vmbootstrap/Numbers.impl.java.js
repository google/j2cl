/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Numbers$impl');


let $Long = goog.require('nativebootstrap.Long$impl');

let Character = goog.forwardDeclare('java.lang.Character$impl');
let Class = goog.forwardDeclare('java.lang.Class$impl');
let Double = goog.forwardDeclare('java.lang.Double$impl');
let Number = goog.forwardDeclare('java.lang.Number$impl');
let Object = goog.forwardDeclare('java.lang.Object$impl');
let $double = goog.forwardDeclare('vmbootstrap.primitives.$double$impl');


/**
 * Provides devirtualized method implementations for Numbers.
 */
class Numbers {
  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   */
  static m_byteValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Double.m_byteValue__java_lang_Double(/**@type {number}*/ (obj));
    }
    return obj.m_byteValue__();
  }

  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   */
  static m_doubleValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Double.m_doubleValue__java_lang_Double(/**@type {number}*/ (obj));
    }
    return obj.m_doubleValue__();
  }

  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   */
  static m_floatValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Double.m_floatValue__java_lang_Double(/**@type {number}*/ (obj));
    }
    return obj.m_floatValue__();
  }

  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   */
  static m_intValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Double.m_intValue__java_lang_Double(/**@type {number}*/ (obj));
    }
    return obj.m_intValue__();
  }

  /**
   * @param {Number|number} obj
   * @return {!$Long}
   * @public
   */
  static m_longValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Double.m_longValue__java_lang_Double(/**@type {number}*/ (obj));
    }
    return obj.m_longValue__();
  }

  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   */
  static m_shortValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Double.m_shortValue__java_lang_Double(/**@type {number}*/ (obj));
    }
    return obj.m_shortValue__();
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    Character = goog.module.get('java.lang.Character$impl');
    Class = goog.module.get('java.lang.Class$impl');
    Double = goog.module.get('java.lang.Double$impl');
    Number = goog.module.get('java.lang.Number$impl');
    Object = goog.module.get('java.lang.Object$impl');
    $double = goog.module.get('vmbootstrap.primitives.$double$impl');
  }
};



/**
 * Exported class.
 */
exports = Numbers;
