/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Comparables$impl');


let Boolean = goog.forwardDeclare('gen.java.lang.Boolean$impl');
let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let Comparable = goog.forwardDeclare('gen.java.lang.Comparable$impl');
let Double = goog.forwardDeclare('gen.java.lang.Double$impl');
let String = goog.forwardDeclare('gen.java.lang.String$impl');
let $boolean = goog.forwardDeclare('vmbootstrap.primitives.$boolean$impl');
let $double = goog.forwardDeclare('vmbootstrap.primitives.$double$impl');


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
    Comparables.$clinit();
    var type = typeof a;
    if (type == 'number') {
      return Double.m_compareTo__java_lang_Double__java_lang_Object(
          /**@type {number} */ (a), /**@type {number} */ (b));
    } else if (type == 'boolean') {
      return Boolean.m_compareTo__java_lang_Boolean__java_lang_Object(
          /**@type {boolean} */ (a), /**@type {boolean} */ (b));
    } else if (type == 'string') {
      return String.m_compareTo__java_lang_String__java_lang_Object(
          /**@type {string} */ (a), /**@type {string} */ (b));
    }
    return a.m_compareTo__java_lang_Object(b);
  }

  /**
   * @param {*} obj
   * @param {*} other
   * @return {boolean}
   * @public
   */
  static m_equals__java_lang_Comparable__java_lang_Object(obj, other) {
    Comparables.$clinit();
    var type = typeof obj;
    if (type == 'number') {
      return Double.m_equals__java_lang_Double__java_lang_Object(
          /**@type {?number}*/ (obj), other);
    } else if (type == 'boolean') {
      return Boolean.m_equals__java_lang_Boolean__java_lang_Object(
          /**@type {?boolean} */ (obj), other);
    } else if (type == 'string') {
      return String.m_equals__java_lang_String__java_lang_Object(
          /**@type {?string}*/ (obj), other);
    } else {
      return obj.m_equals__java_lang_Object(other);
    }
  }

  /**
   * @param {*} obj
   * @return {number}
   * @public
   */
  static m_hashCode__java_lang_Comparable(obj) {
    Comparables.$clinit();
    var type = typeof obj;
    if (type == 'number') {
      return Double.m_hashCode__java_lang_Double(/**@type {?number}*/ (obj));
    } else if (type == 'boolean') {
      return Boolean.m_hashCode__java_lang_Boolean(/**@type {boolean} */ (obj));
    } else if (typeof obj == 'string') {
      return String.m_hashCode__java_lang_String(/**@type {?string}*/ (obj));
    } else {
      return obj.m_hashCode();
    }
  }

  /**
   * @param {*} obj
   * @return {?string}
   * @public
   */
  static m_toString__java_lang_Comparable(obj) {
    Comparables.$clinit();
    var type = typeof obj;
    if (type == 'number') {
      return obj.toString();
    } else if (type == 'boolean') {
      return obj.toString();
    } else if (typeof obj == 'string') {
      return obj.toString();
    } else {
      return obj.m_toString();
    }
  }

  /**
   * @param {*} obj
   * @return {Class}
   * @public
   */
  static m_getClass__java_lang_Comparable(obj) {
    Comparables.$clinit();
    var type = typeof obj;
    if (type == 'number') {
      return $double.$getClass();
    } else if (type == 'boolean') {
      return $boolean.$getClass();
    } else if (typeof obj == 'string') {
      return String.$getClass();
    } else {
      return obj.m_getClass();
    }
  }

  /**
   * Runs inline static field initializers.
   * @public
   * @nocollapse
   */
  static $clinit() {
    Boolean = goog.module.get('gen.java.lang.Boolean$impl');
    Class = goog.module.get('gen.java.lang.Class$impl');
    Comparable = goog.module.get('gen.java.lang.Comparable$impl');
    Double = goog.module.get('gen.java.lang.Double$impl');
    String = goog.module.get('gen.java.lang.String$impl');
    $boolean = goog.module.get('vmbootstrap.primitives.$boolean$impl');
    $double = goog.module.get('vmbootstrap.primitives.$double$impl');
  }
};


/**
 * Exported class.
 */
exports = Comparables;
