/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Objects$impl');


let Boolean = goog.forwardDeclare('gen.java.lang.Boolean$impl');
let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let Double = goog.forwardDeclare('gen.java.lang.Double$impl');
let String = goog.forwardDeclare('gen.java.lang.String$impl');
let Arrays = goog.forwardDeclare('vmbootstrap.Arrays$impl');
let $boolean = goog.forwardDeclare('vmbootstrap.primitives.$boolean$impl');
let $double = goog.forwardDeclare('vmbootstrap.primitives.$double$impl');


/**
 * Provides devirtualized Object methods
 */
class Objects {
  /**
   * @param {*} obj
   * @param {*} other
   * @return {boolean}
   * @public
   */
  static m_equals__java_lang_Object__java_lang_Object(obj, other) {
    Objects.$clinit();
    let type = typeof obj;
    if (type == 'number') {
      return Double.m_equals__java_lang_Double__java_lang_Object(
          /**@type {?number}*/ (obj), other);
    } else if (type == 'boolean') {
      return Boolean.m_equals__java_lang_Boolean__java_lang_Object(
          /**@type {?boolean}*/ (obj), other);
    } else if (type == 'string') {
      return String.m_equals__java_lang_String__java_lang_Object(
          /**@type {?string}*/ (obj), other);
    } else if (obj instanceof Array) {
      return Arrays.m_equals__java_lang_Object__java_lang_Object(obj, other);
    } else {
      return obj.m_equals__java_lang_Object(other);
    }
  }

  /**
   * @param {*} obj
   * @return {number}
   * @public
   */
  static m_hashCode__java_lang_Object(obj) {
    Objects.$clinit();
    let type = typeof obj;
    if (type == 'number') {
      return Double.m_hashCode__java_lang_Double(/**@type {?number}*/ (obj));
    } else if (type == 'boolean') {
      return Boolean.m_hashCode__java_lang_Boolean(/**@type {?boolean}*/ (obj));
    } else if (type == 'string') {
      return String.m_hashCode__java_lang_String(/**@type {?string}*/ (obj));
    } else if (obj instanceof Array) {
      return Arrays.m_hashCode__java_lang_Object(obj);
    } else {
      return obj.m_hashCode();
    }
  }

  /**
   * @param {*} obj
   * @return {?string}
   * @public
   */
  static m_toString__java_lang_Object(obj) {
    Objects.$clinit();
    let type = typeof obj;
    if (type == 'number') {
      return obj.toString();
    } else if (type == 'boolean') {
      return obj.toString();
    } else if (type == 'string') {
      return obj.toString();
    } else if (obj instanceof Array) {
      return Arrays.m_toString__java_lang_Object(obj);
    } else {
      return obj.m_toString();
    }
  }

  /**
   * @param {*} obj
   * @return {Class}
   * @public
   */
  static m_getClass__java_lang_Object(obj) {
    Objects.$clinit();
    let type = typeof obj;
    if (type == 'number') {
      return $double.$getClass();
    } else if (type == 'boolean') {
      return $boolean.$getClass();
    } else if (type == 'string') {
      return String.$getClass();
    } else if (obj instanceof Array) {
      return Arrays.m_getClass__java_lang_Object(obj);
    } else {
      return obj.m_getClass();
    }
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    Boolean = goog.module.get('gen.java.lang.Boolean$impl');
    Class = goog.module.get('gen.java.lang.Class$impl');
    Double = goog.module.get('gen.java.lang.Double$impl');
    String = goog.module.get('gen.java.lang.String$impl');
    Arrays = goog.module.get('vmbootstrap.Arrays$impl');
    $boolean = goog.module.get('vmbootstrap.primitives.$boolean$impl');
    $double = goog.module.get('vmbootstrap.primitives.$double$impl');
  }
};


/**
 * Exported class.
 */
exports = Objects;
