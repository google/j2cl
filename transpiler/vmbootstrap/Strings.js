goog.module('vmbootstrap.StringsModule');


let Class = goog.require('gen.java.lang.CoreModule').Class;
let String = goog.require('gen.java.lang.StringModule').String;
let $Casts = goog.require('vmbootstrap.CastsModule').Casts;


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
    var hashCode = 0;
    var len = obj.length;
    for (var i = 0; i < len; i++) {
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
    return Strings.m_compareTo__java_lang_String__java_lang_String(
      a, /**@type {string} */ ($Casts.to(b, String.$isInstance(b))));
  }
};


/**
 * Exported class.
 */
exports.Strings = Strings;
