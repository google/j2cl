goog.module('vmbootstrap.NumbersModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var Number = goog.require('gen.java.lang.NumberModule').Number;
var Long = goog.require('nativebootstrap.LongUtilsModule').Long;
var $double = goog.require('vmbootstrap.PrimitivesModule').$double;
var Primitives = goog.require('vmbootstrap.PrimitivesModule').Primitives;

/**
 * Provides devirtualized method implementations for Numbers.
 */
class Numbers {
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
    return /** @type {number} */ (obj);
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
    return $double.$class;
  }

  /**
   * @param {Number|number|!Long} obj
   * @return {number}
   * @public
   */
  static m_byteValue__java_lang_Number(obj) {
    if (typeof obj == 'number') {
      return Primitives.$castDoubleToByte(obj);
    } else if (obj instanceof Long) {
      return Primitives.$castLongToByte(obj);
    } else {
      return obj.m_byteValue();
    }
  }

  /**
   * @param {Number|number|!Long} obj
   * @return {number}
   * @public
   */
  static m_doubleValue__java_lang_Number(obj) {
    if (typeof obj == 'number') {
      return obj;
    } else if (obj instanceof Long) {
      return Primitives.$castLongToDouble(obj);
    } else {
      return obj.m_doubleValue();
    }
  }

  /**
   * @param {Number|number|!Long} obj
   * @return {number}
   * @public
   */
  static m_floatValue__java_lang_Number(obj) {
    if (typeof obj == 'number') {
      return obj;
    } else if (obj instanceof Long) {
      return Primitives.$castLongToFloat(obj);
    } else {
      return obj.m_floatValue();
    }
  }

  /**
   * @param {Number|number|!Long} obj
   * @return {number}
   * @public
   */
  static m_intValue__java_lang_Number(obj) {
    if (typeof obj == 'number') {
      return Primitives.$castDoubleToInt(obj);
    } else if (obj instanceof Long) {
      return Primitives.$castLongToInt(obj);
    } else {
      return obj.m_intValue();
    }
  }

  /**
   * @param {Number|number|!Long} obj
   * @return {!Long}
   * @public
   */
  static m_longValue__java_lang_Number(obj) {
    if (typeof obj == 'number') {
      return Primitives.$castDoubleToLong(obj);
    } else if (obj instanceof Long) {
      return /**@type{!Long} */ (obj);
    } else {
      return obj.m_longValue();
    }
  }

  /**
   * @param {Number|number|!Long} obj
   * @return {number}
   * @public
   */
  static m_shortValue__java_lang_Number(obj) {
    if (typeof obj == 'number') {
      return Primitives.$castDoubleToShort(obj);
    } else if (obj instanceof Long) {
      return Primitives.$castLongToShort(obj);
    } else {
      return obj.m_shortValue();
    }
  }
};


/**
 * Exported class.
 */
exports.Numbers = Numbers;
