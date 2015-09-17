goog.module('vmbootstrap.BooleansModule');


let Boolean = goog.require('gen.java.lang.BooleanModule').Boolean;
let Class = goog.require('gen.java.lang.CoreModule').Class;
let $Casts = goog.require('vmbootstrap.CastsModule').Casts;
let $boolean = goog.require('vmbootstrap.PrimitivesModule').$boolean;

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
    return $boolean.$getClass();
  }

  /**
   * @param {?boolean} obj
   * @return {boolean}
   * @public
   */
  static m_booleanValue__java_lang_Boolean(obj) {
    if (obj != null) {
      return obj;
    } else {
      return obj.m_booleanValue();
    }
  }

  /**
   * @param {?boolean} a
   * @param {?boolean} b
   * @return {number}
   * @public
   */
  static m_compareTo__java_lang_Boolean__java_lang_Boolean(a, b) {
    if (a != null) {
      if (b != null) {
        return Boolean.m_compareTo__boolean__boolean(a, b);
      } else {
        return Boolean.m_compareTo__boolean__boolean(a, b.m_booleanValue());
      }
    } else {
      return a.m_compareTo__java_lang_Boolean(b);
    }
  }

  /**
   * @param {?boolean} a
   * @param {*} b
   * @return {number}
   * @public
   */
  static m_compareTo__java_lang_Boolean__java_lang_Object(a, b) {
    return Booleans.m_compareTo__java_lang_Boolean__java_lang_Boolean(
      a, /**@type {boolean} */ ($Casts.to(b, Boolean.$isInstance(b))));
  }
};


/**
 * Exported class.
 */
exports.Booleans = Booleans;
