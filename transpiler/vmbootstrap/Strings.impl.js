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

  //==================================
  // Comparable Methods
  //==================================

  /**
   * @param {?string} a
   * @param {?string} b
   * @return {number}
   * @public
   */
  static m_compareTo__java_lang_String__java_lang_String(a, b) {
    Strings.$clinit();

    // Generate Null Pointer Exceptions for a and b if they are null
    if (a == null) {
      a.m_compareTo__java_lang_String(b);
    }
    if (b == null) {
      b.m_compareTo__java_lang_String(a);
    }

    if (a == b) {
      return 0;
    }
    return a < b ? -1 : 1;
  }

  /**
   * @param {?string} a
   * @param {*} b
   * @return {number}
   * @public
   */
  static m_compareTo__java_lang_String__java_lang_Object(a, b) {
    Strings.$clinit();
    return Strings.m_compareTo__java_lang_String__java_lang_String(
        a, /**@type {string} */ ($Casts.to(b, String)));
  }

  //==================================
  // CharSequence Methods
  //==================================

  /**
   * @param {string} obj
   * @return {number}
   * @public
   */
  static m_length__java_lang_String(obj) { return obj.length; }

  /**
   * @param {string} obj
   * @param {number} index
   * @return {number}
   */
  static m_charAt__java_lang_String__int(obj, index) {
    return obj.charCodeAt(index);
  }

  /**
   * @param {string} obj
   * @param {number} start
   * @param {number} end
   * @return {string}
   */
  static m_subSequence__java_lang_String__int__int(obj, start, end) {
    return obj.substring(start, end);
  }

  //==================================
  // String Instance Methods
  //==================================

  /**
   * @param {string} obj
   * @param {number} start
   * @param {number} endIndex
   * @return {string}
   * @public
   */
  static m_substring__java_lang_String__int__int(obj, start, endIndex) {
    return obj.substring(start, endIndex);
  }

  /**
   * @param {string} obj
   * @param {number} start
   * @return {string}
   * @public
   */
  static m_substring__java_lang_String__int(obj, start) {
    return obj.substring(start);
  }

  /**
   *
   * @param {string} obj
   * @return {string}
   * @public
   */
  static m_trim__java_lang_String(obj) { return obj.trim(); }

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
