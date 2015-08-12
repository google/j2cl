goog.module('vmbootstrap.LongsModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var Long = goog.require('nativebootstrap.LongUtilsModule').Long;
var LongUtils = goog.require('nativebootstrap.LongUtilsModule').LongUtils;
var $long = goog.require('vmbootstrap.PrimitivesModule').$long;
var Primitives = goog.require('vmbootstrap.PrimitivesModule').Primitives;

/**
 * Provides devirtualized method implementations for Longs.
 */
class Longs {
  /**
   * @param {!Long} obj
   * @param {*} other
   * @return {boolean}
   * @public
   */
  static m_equals__java_lang_Object__java_lang_Object(obj, other) {
    return (other instanceof Long) &&
      LongUtils.$equals(obj, /** @type {!Long} */ (other));
  }

  /**
   * @param {!Long} obj
   * @return {number}
   * @public
   */
  static m_hashCode__java_lang_Object(obj) {
    return LongUtils.$getLowBits(obj) ^ LongUtils.$getHighBits(obj);
  }

  /**
   * @param {!Long} obj
   * @return {?string}
   * @public
   */
  static m_toString__java_lang_Object(obj) {
    return LongUtils.$toString(obj);
  }

  /**
   * @param {!Long} obj
   * @return {Class}
   * @public
   */
  static m_getClass__java_lang_Object(obj) {
    return $long.$class;
  }

  /**
   * @param {!Long} obj
   * @return {number}
   * @public
   */
  static m_byteValue__java_lang_Long(obj) {
    return Primitives.$castLongToByte(obj);
  }

  /**
   * @param {!Long} obj
   * @return {number}
   * @public
   */
  static m_doubleValue__java_lang_Long(obj) {
    return Primitives.$castLongToDouble(obj);
  }

  /**
   * @param {!Long} obj
   * @return {number}
   * @public
   */
  static m_floatValue__java_lang_Long(obj) {
    return Primitives.$castLongToFloat(obj);
  }

  /**
   * @param {!Long} obj
   * @return {number}
   * @public
   */
  static m_intValue__java_lang_Long(obj) {
    return Primitives.$castLongToInt(obj);
  }

  /**
   * @param {!Long} obj
   * @return {!Long}
   * @public
   */
  static m_longValue__java_lang_Long(obj) {
    return obj;
  }

  /**
   * @param {!Long} obj
   * @return {number}
   * @public
   */
  static m_shortValue__java_lang_Long(obj) {
    return Primitives.$castLongToShort(obj);
  }

  /**
   * @param {!Long} obj
   * @return {!Long}
   * @public
   */
  static $create__long(obj) {
    return obj;
  }
};


/**
 * Exported class.
 */
exports.Longs = Longs;
