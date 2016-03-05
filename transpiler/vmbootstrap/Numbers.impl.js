/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Numbers$impl');


let $Long = goog.require('nativebootstrap.Long$impl');

let Character = goog.forwardDeclare('gen.java.lang.Character$impl');
let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let Double = goog.forwardDeclare('gen.java.lang.Double$impl');
let Number = goog.forwardDeclare('gen.java.lang.Number$impl');
let Object = goog.forwardDeclare('gen.java.lang.Object$impl');
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
    return obj.m_byteValue();
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
    return obj.m_doubleValue();
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
    return obj.m_floatValue();
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
    return obj.m_intValue();
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
    return obj.m_longValue();
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
    return obj.m_shortValue();
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    Character = goog.module.get('gen.java.lang.Character$impl');
    Class = goog.module.get('gen.java.lang.Class$impl');
    Double = goog.module.get('gen.java.lang.Double$impl');
    Number = goog.module.get('gen.java.lang.Number$impl');
    Object = goog.module.get('gen.java.lang.Object$impl');
    $double = goog.module.get('vmbootstrap.primitives.$double$impl');
  }
};


/**
 * Used to store qualifier of a boxed object to avoid double side effects.
 * @public {*}
 */
Numbers.$q = null;


/**
 * Used to store pre-modified value of a boxed object in a postfix expression.
 * @public {number | Number | Character | $Long}
 */
Numbers.$v = null;


/**
 * Exported class.
 */
exports = Numbers;
