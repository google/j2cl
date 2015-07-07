goog.module('vmbootstrap.DoublesModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var $double = goog.require('vmbootstrap.PrimitivesModule').$double;

// TODO(rluble): Maybe this should be merged with $double
/**
 * Provides devirtualized method implementations for Doubles.
 */
class Doubles {
  /**
   * @param {?} obj
   * @param {?} other
   * @return {boolean}
   * @public
   */
  static m_equals__java_lang_Object__java_lang_Object(obj, other) {
    return obj === other;
  }

  /**
   * @param {?} obj
   * @return {number}
   * @public
   */
  static m_hashCode__java_lang_Object(obj) {
    return obj;
  }

  /**
   * @param {?} obj
   * @return {?string}
   * @public
   */
  static m_toString__java_lang_Object(obj) {
    return obj.toString();
  }

  /**
   * @param {?} obj
   * @return {Class}
   * @public
   */
  static m_getClass__java_lang_Object(obj) {
    return $double.$class;
  }
};


/**
 * Exported class.
 */
exports.Doubles = Doubles;
