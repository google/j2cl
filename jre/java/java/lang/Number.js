goog.module('gen.java.lang.NumberModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var Object = goog.require('gen.java.lang.CoreModule').Object;
var $Long = goog.require('nativebootstrap.LongUtilsModule').Long;
var $Util = goog.require('nativebootstrap.UtilModule').Util;
var $long = goog.require('vmbootstrap.PrimitivesModule').$long;
var $Primitives = goog.require('vmbootstrap.PrimitivesModule').Primitives;


/**
 * Super-sourcing java/lang/Number.java.
 */
class Number extends Object {
  /**
   * Defines instance fields.
   * @private
   */
  constructor() {
    super();
  }

  /**
   * Runs instance field and block initializers.
   * @private
   */
  $init__java_lang_Number() {
  }

  /**
   * @return {number}
   * @public
   */
  m_byteValue() {
    return $Primitives.$castIntToByte(this.m_intValue());
  }

  /**
   * @return {number}
   * @public
   */
  m_doubleValue() {
  }

  /**
   * @return {number}
   * @public
   */
  m_floatValue() {
  }

  /**
   * @return {number}
   * @public
   */
  m_intValue() {
  }

  /**
   * @return {!$Long}
   * @public
   */
  m_longValue() {
  }

  /**
   * @return {number}
   * @public
   */
  m_shortValue() {
    return $Primitives.$castIntToShort(this.m_intValue());
  }

  /**
   * A particular Java constructor as a factory method.
   * @return {!Number}
   * @public
   * @nocollapse
   */
  static $create() {
    Number.$clinit();
    var instance = new Number;
    instance.$ctor__java_lang_Number();
    return instance;
  }

  /**
   * Initializes instance fields for a particular Java constructor.
   * @protected
   */
  $ctor__java_lang_Number() {
    this.$ctor__java_lang_Object();
    this.$init__java_lang_Number();
  }

  /**
   * Returns whether the provided instance is an instance of this class.
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isInstance(instance) {
    return typeof instance == 'number' || instance instanceof Number;
  }

  /**
   * Returns whether the provided class is or extends this class.
   * @param {Function} classConstructor
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return $Util.$canCastClass(classConstructor, Number);
  }

  /**
   * Runs inline static field initializers.
   * @protected
   * @nocollapse
   */
  static $clinit() {
    Object.$clinit();
  }};


/**
 * @public {Class}
 * @nocollapse
 */
Number.$class = Class.$createForClass(
  $Util.$generateId('Number'),
  $Util.$generateId('java.lang.Number'),
  Object.$class,
  $Util.$generateId('java.lang.Number'));

/**
 * Export class.
 */
exports.Number = Number;
