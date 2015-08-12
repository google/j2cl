goog.module('vmbootstrap.ObjectsModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var Arrays = goog.require('vmbootstrap.ArraysModule').Arrays;
var Longs = goog.require('vmbootstrap.LongsModule').Longs;
var Numbers = goog.require('vmbootstrap.NumbersModule').Numbers;
var Strings = goog.require('vmbootstrap.StringsModule').Strings;
var Long = goog.require('nativebootstrap.LongUtilsModule').Long;


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
    var type = typeof obj;
    if (type == 'number') {
      return Numbers.m_equals__java_lang_Object__java_lang_Object(obj, other);
    } else if (obj instanceof Long) {
      return Longs.m_equals__java_lang_Object__java_lang_Object(obj, other);
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
    var type = typeof obj;
    if (type == 'number') {
      return Numbers.m_hashCode__java_lang_Object(obj);
    } else if (obj instanceof Long) {
      return Longs.m_hashCode__java_lang_Object(obj);
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
    var type = typeof obj;
    if (type == 'number') {
      return Numbers.m_toString__java_lang_Object(obj);
    } else if (obj instanceof Long) {
      return Longs.m_toString__java_lang_Object(obj);
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
    var type = typeof obj;
    if (type == 'number') {
      return Numbers.m_getClass__java_lang_Object(obj);
    } else if (obj instanceof Long) {
      return Longs.m_getClass__java_lang_Object(obj);
    } else if (type == 'string') {
      return Strings.m_getClass__java_lang_Object(obj);
    } else if (obj instanceof Array) {
      return Arrays.m_getClass__java_lang_Object(obj);
    } else {
      return obj.m_getClass();
    }
  }
};


/**
 * Exported class.
 */
exports.Objects = Objects;
