goog.module('vmbootstrap.ShortsModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var Long = goog.require('nativebootstrap.LongUtilsModule').Long;
var $short = goog.require('vmbootstrap.PrimitivesModule').$short;
var Primitives = goog.require('vmbootstrap.PrimitivesModule').Primitives;

/**
 * Provides devirtualized method implementations for Shorts.
 */
class Shorts {
  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_byteValue__java_lang_Short(obj) {
    return Primitives.$castShortToByte(obj);
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_doubleValue__java_lang_Short(obj) {
    return obj;
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_floatValue__java_lang_Short(obj) {
    return obj;
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_intValue__java_lang_Short(obj) {
    return obj;
  }

  /**
   * @param {number} obj
   * @return {!Long}
   * @public
   */
  static m_longValue__java_lang_Short(obj) {
    return Primitives.$castShortToLong(obj);
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static m_shortValue__java_lang_Short(obj) {
    return obj;
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   */
  static $create__short(obj) {
    return obj;
  }
};


/**
 * Exported class.
 */
exports.Shorts = Shorts;
