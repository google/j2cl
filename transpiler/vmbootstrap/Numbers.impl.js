/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Numbers$impl');


let $Long = goog.require('nativebootstrap.Long$impl');

let Character = goog.forwardDeclare('gen.java.lang.Character$impl');
let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let Double = goog.forwardDeclare('gen.java.lang.Double$impl');
let Number = goog.forwardDeclare('gen.java.lang.Number$impl');
let $Casts = goog.forwardDeclare('vmbootstrap.Casts$impl');
let $double = goog.forwardDeclare('vmbootstrap.primitives.$double$impl');
let Primitives = goog.forwardDeclare('vmbootstrap.primitives.Primitives$impl');


/**
 * Provides devirtualized method implementations for Numbers.
 */
class Numbers {
  /**
   * @param {*} obj
   * @param {*} other
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static m_equals__java_lang_Object__java_lang_Object(obj, other) {
    return obj === other;
  }

  /**
   * @param {*} obj
   * @return {number}
   * @public
   * @nocollapse
   */
  static m_hashCode__java_lang_Object(obj) {
    return /** @type {number} */ (obj);
  }

  /**
   * @param {*} obj
   * @return {?string}
   * @public
   * @nocollapse
   */
  static m_toString__java_lang_Object(obj) {
    return  /** @type {Object} */ (obj).toString();
  }

  /**
   * @param {*} obj
   * @return {Class}
   * @public
   * @nocollapse
   */
  static m_getClass__java_lang_Object(obj) {
    Numbers.$clinit();
    return $double.$getClass();
  }

  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   * @nocollapse
   */
  static m_byteValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Primitives.$narrowDoubleToByte(obj);
    } else {
      return obj.m_byteValue();
    }
  }

  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   * @nocollapse
   */
  static m_doubleValue__java_lang_Number(obj) {
    if (typeof obj == 'number') {
      return obj;
    } else {
      return obj.m_doubleValue();
    }
  }

  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   * @nocollapse
   */
  static m_floatValue__java_lang_Number(obj) {
    if (typeof obj == 'number') {
      return obj;
    } else {
      return obj.m_floatValue();
    }
  }

  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   * @nocollapse
   */
  static m_intValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Primitives.$narrowDoubleToInt(obj);
    } else {
      return obj.m_intValue();
    }
  }

  /**
   * @param {Number|number} obj
   * @return {!$Long}
   * @public
   * @nocollapse
   */
  static m_longValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Primitives.$narrowDoubleToLong(obj);
    } else {
      return obj.m_longValue();
    }
  }

  /**
   * @param {Number|number} obj
   * @return {number}
   * @public
   * @nocollapse
   */
  static m_shortValue__java_lang_Number(obj) {
    Numbers.$clinit();
    if (typeof obj == 'number') {
      return Primitives.$narrowDoubleToShort(obj);
    } else {
      return obj.m_shortValue();
    }
  }

  /**
   * @param {?number} a
   * @param {?number} b
   * @return {number}
   * @public
   */
  static m_compareTo__java_lang_Number__java_lang_Double(a, b) {
    Numbers.$clinit();

    // Generate Null Pointer Exceptions for a and b if they are null
    if (a == null) {
      a.m_compareTo__java_lang_Double(b);
    }
    if (b == null) {
      b.m_compareTo__java_lang_Double(a);
    }

    return Double.m_compare__double__double(a, b);
  }

  /**
   * @param {?number} a
   * @param {*} b
   * @return {number}
   * @public
   */
  static m_compareTo__java_lang_Number__java_lang_Object(a, b) {
    Numbers.$clinit();
    return Numbers.m_compareTo__java_lang_Number__java_lang_Double(
      a, /**@type {number} */ ($Casts.to(b, Double)));
  }

  /**
   * Runs inline static field initializers.
   * @protected
   * @nocollapse
   */
  static $clinit() {
    Character = goog.module.get('gen.java.lang.Character$impl');
    Class = goog.module.get('gen.java.lang.Class$impl');
    Double = goog.module.get('gen.java.lang.Double$impl');
    Number = goog.module.get('gen.java.lang.Number$impl');
    $Casts = goog.module.get('vmbootstrap.Casts$impl');
    $double = goog.module.get('vmbootstrap.primitives.$double$impl');
    Primitives = goog.module.get('vmbootstrap.primitives.Primitives$impl');
  }
};


/**
 * Used to store qualifier of a boxed object to avoid double side effects.
 * @public {*}
 */
Numbers.$q = null;


/**
 * Used to store pre-modified value of a boxed object in a postfix expression.
 * @public {number | Number | Character | $Long}
 */
Numbers.$v = null;


/**
 * Exported class.
 */
exports = Numbers;
