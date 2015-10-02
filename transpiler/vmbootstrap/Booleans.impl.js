/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Booleans$impl');


let Boolean = goog.forwardDeclare('gen.java.lang.Boolean$impl');
let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let $boolean = goog.forwardDeclare('vmbootstrap.primitives.$boolean$impl');
let $Casts = goog.forwardDeclare('vmbootstrap.Casts$impl');


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
    Booleans.$clinit();
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
    Booleans.$clinit();

    // Generate Null Pointer Exceptions for a and b if they are null
    if (a == null) {
      a.m_compareTo__boolean(b);
    }
    if (b == null) {
      b.m_compareTo__boolean(a);
    }

    return Boolean.m_compare__boolean__boolean(a, b);
  }

  /**
   * @param {?boolean} a
   * @param {*} b
   * @return {number}
   * @public
   */
  static m_compareTo__java_lang_Boolean__java_lang_Object(a, b) {
    Booleans.$clinit();
    return Booleans.m_compareTo__java_lang_Boolean__java_lang_Boolean(
      a, /**@type {boolean} */ ($Casts.to(b, Boolean)));
  }

  /**
   * Runs inline static field initializers.
   * @protected
   * @nocollapse
   */
  static $clinit() {
    Boolean = goog.module.get('gen.java.lang.Boolean$impl');
    Class = goog.module.get('gen.java.lang.Class$impl');
    $boolean = goog.module.get('vmbootstrap.primitives.$boolean$impl');
    $Casts = goog.module.get('vmbootstrap.Casts$impl');
  }
};


/**
 * Exported class.
 */
exports = Booleans;
