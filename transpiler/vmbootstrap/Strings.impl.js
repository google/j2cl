/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Strings$impl');


let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let String = goog.forwardDeclare('gen.java.lang.String$impl');
let $Casts = goog.forwardDeclare('vmbootstrap.Casts$impl');


/**
 * Provides devirtualized method implementations for Strings.
 */
class Strings {
  /**
   * @param {*} obj
   * @param {*} other
   * @return {boolean}
   * @public
   */
  static m_equals__java_lang_Object__java_lang_Object(obj, other) {
    return obj === other;
  }

  /**
   * @param {*} obj
   * @return {number}
   * @public
   */
  static m_hashCode__java_lang_Object(obj) {
    let hashCode = 0;
    let len = obj.length;
    for (let i = 0; i < len; i++) {
      hashCode += obj.charCodeAt(i) * Math.pow(31, len - i - 1);
    }
    return hashCode;
  }

  /**
   * @param {*} obj
   * @return {?string}
   * @public
   */
  static m_toString__java_lang_Object(obj) {
    return /** @type {?string} */ (obj);
  }

  /**
   * @param {*} obj
   * @return {Class}
   * @public
   */
  static m_getClass__java_lang_Object(obj) {
    Strings.$clinit();
    return String.$getClass();
  }

  /**
   * @param {string} a
   * @param {string} b
   * @return {number}
   * @public
   */
  static m_compareTo__java_lang_String__java_lang_String(a, b) {
    if (a == b) {
      return 0;
    }
    return a < b ? -1 : 1;
  }

  /**
   * @param {string} a
   * @param {*} b
   * @return {number}
   * @public
   */
  static m_compareTo__java_lang_String__java_lang_Object(a, b) {
    Strings.$clinit();
    return Strings.m_compareTo__java_lang_String__java_lang_String(
      a, /**@type {string} */ ($Casts.to(b, String.$isInstance(b))));
  }

  /**
   * Runs inline static field initializers.
   * @protected
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
    String = goog.module.get('gen.java.lang.String$impl');
    $Casts = goog.module.get('vmbootstrap.Casts$impl');
  }
};


/**
 * Exported class.
 */
exports = Strings;
