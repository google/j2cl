goog.module('vmbootstrap.IntegersModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var Long = goog.require('nativebootstrap.LongUtilsModule').Long;
var $int = goog.require('vmbootstrap.PrimitivesModule').$int;
var Primitives = goog.require('vmbootstrap.PrimitivesModule').Primitives;

/**
 * Provides devirtualized method implementations for Integers.
 */
class Integers {
  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_byteValue__java_lang_Integer(obj) {
    return Primitives.$castIntToByte(obj);
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_doubleValue__java_lang_Integer(obj) {
    return obj;
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_floatValue__java_lang_Integer(obj) {
    return obj;
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_intValue__java_lang_Integer(obj) {
    return obj;
  }

  /**
   * @param {number} obj
   * @return {!Long}
   * @public
   */
  static m_longValue__java_lang_Integer(obj) {
    return Primitives.$castIntToLong(obj);
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_shortValue__java_lang_Integer(obj) {
    return Primitives.$castIntToShort(obj);
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static $create__int(obj) {
    return obj;
  }
};


/**
 * Exported class.
 */
exports.Integers = Integers;
