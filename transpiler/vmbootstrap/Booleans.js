goog.module('vmbootstrap.BooleansModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var $boolean = goog.require('vmbootstrap.PrimitivesModule').$boolean;

/**
 * Provides devirtualized method implementations for Booleans.
 */
class Booleans {
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
    return obj ? 1231 : 1237;
  }

  /**
   * @param {*} obj
   * @return {?string}
   * @public
   */
  static m_toString__java_lang_Object(obj) {
    return  /** @type {Object} */ (obj).toString();
  }

  /**
   * @param {*} obj
   * @return {Class}
   * @public
   */
  static m_getClass__java_lang_Object(obj) {
    return $boolean.$class;
  }

  /**
   * @param {boolean} obj
   * @return {boolean}
   * @public
   */
  static m_booleanValue__boolean(obj) {
    return obj;
  }
};


/**
 * Exported class.
 */
exports.Booleans = Booleans;
