/**
 * Impl super source for java.lang.Number.
 */
goog.module('gen.java.lang.Number$impl');


let Object = goog.require('gen.java.lang.Object$impl');
let $Long = goog.require('nativebootstrap.Long$impl');
let $Util = goog.require('nativebootstrap.Util$impl');

let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let $long = goog.forwardDeclare('vmbootstrap.primitives.$long$impl');
let $Primitives = goog.forwardDeclare('vmbootstrap.primitives.Primitives$impl');


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
    let instance = new Number;
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
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    Number.$clinit();
    if (!Number.$classNumber) {
      Number.$classNumber = Class.$createForClass(
          $Util.$generateId('Number'),
          $Util.$generateId('java.lang.Number'),
          Object.$getClass(),
          $Util.$generateId('java.lang.Number'));
    }
    return Number.$classNumber;
  }

  /**
   * Runs inline static field initializers.
   * @protected
   * @nocollapse
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
    $long = goog.module.get('vmbootstrap.primitives.$long$impl');
    $Primitives = goog.module.get('vmbootstrap.primitives.Primitives$impl');
    Object.$clinit();
  }
};


/**
 * Export class.
 */
exports = Number;
