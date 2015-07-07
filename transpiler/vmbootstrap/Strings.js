goog.module('vmbootstrap.StringsModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var String = goog.require('gen.java.lang.StringModule').String;


/**
 * Provides devirtualized method implementations for Strings.
 */
class Strings {
  /**
   * @param {?} obj
   * @param {?} other
   * @return {boolean}
   * @public
   */
  static m_equals__java_lang_Object__java_lang_Object(obj, other) {
    return obj === other;
  }

  /**
   * @param {?} obj
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
   * @param {?} obj
   * @return {?string}
   * @public
   */
  static m_toString__java_lang_Object(obj) {
    return obj;
  }

  /**
   * @param {?} obj
   * @return {Class}
   * @public
   */
  static m_getClass__java_lang_Object(obj) {
    return String.$class;
  }
};


/**
 * Exported class.
 */
exports.Strings = Strings;
