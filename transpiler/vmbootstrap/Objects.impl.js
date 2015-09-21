/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Objects$impl');


let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let Arrays = goog.forwardDeclare('vmbootstrap.Arrays$impl');
let Booleans = goog.forwardDeclare('vmbootstrap.Booleans$impl');
let Numbers = goog.forwardDeclare('vmbootstrap.Numbers$impl');
let Strings = goog.forwardDeclare('vmbootstrap.Strings$impl');


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
      return Numbers.m_equals__java_lang_Object__java_lang_Object(obj, other);
    } else if (type == 'boolean') {
      return Booleans.m_equals__java_lang_Object__java_lang_Object(obj, other);
    } else if (type == 'string') {
      return Strings.m_equals__java_lang_Object__java_lang_Object(obj, other);
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
      return Numbers.m_hashCode__java_lang_Object(obj);
    } else if (type == 'boolean') {
      return Booleans.m_hashCode__java_lang_Object(obj);
    } else if (type == 'string') {
      return Strings.m_hashCode__java_lang_Object(obj);
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
      return Numbers.m_toString__java_lang_Object(obj);
    } else if (type == 'boolean') {
      return Booleans.m_toString__java_lang_Object(obj);
    } else if (type == 'string') {
      return Strings.m_toString__java_lang_Object(obj);
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
      return Numbers.m_getClass__java_lang_Object(obj);
    } else if (type == 'boolean') {
      return Booleans.m_getClass__java_lang_Object(obj);
    } else if (type == 'string') {
      return Strings.m_getClass__java_lang_Object(obj);
    } else if (obj instanceof Array) {
      return Arrays.m_getClass__java_lang_Object(obj);
    } else {
      return obj.m_getClass();
    }
  }

  /**
   * Runs inline static field initializers.
   * @protected
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
    Arrays = goog.module.get('vmbootstrap.Arrays$impl');
    Booleans = goog.module.get('vmbootstrap.Booleans$impl');
    Numbers = goog.module.get('vmbootstrap.Numbers$impl');
    Strings = goog.module.get('vmbootstrap.Strings$impl');
  }
};


/**
 * Exported class.
 */
exports = Objects;
