goog.module('vmbootstrap.FloatsModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var Long = goog.require('nativebootstrap.LongUtilsModule').Long;
var $float = goog.require('vmbootstrap.PrimitivesModule').$float;
var Primitives = goog.require('vmbootstrap.PrimitivesModule').Primitives;

/**
 * Provides devirtualized method implementations for Floats.
 */
class Floats {
  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_byteValue__java_lang_Float(obj) {
    return Primitives.$castFloatToByte(obj);
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_doubleValue__java_lang_Float(obj) {
    return obj;
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_floatValue__java_lang_Float(obj) {
    return obj;
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_intValue__java_lang_Float(obj) {
    return Primitives.$castFloatToInt(obj);
  }

  /**
   * @param {number} obj
   * @return {!Long}
   * @public
   */
  static m_longValue__java_lang_Float(obj) {
    return Primitives.$castFloatToLong(obj);
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_shortValue__java_lang_Float(obj) {
    return Primitives.$castFloatToShort(obj);
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static $create__float(obj) {
    return obj;
  }
};


/**
 * Exported class.
 */
exports.Floats = Floats;
