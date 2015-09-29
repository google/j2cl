/**
 * Impl super source for java.lang.Double.
 */
goog.module('gen.java.lang.Double$impl');


let Number = goog.require('gen.java.lang.Number$impl');
let $Util = goog.require('nativebootstrap.Util$impl');

let Class = goog.forwardDeclare('gen.java.lang.Class$impl');


class Double extends Number {
  /**
   * Defines instance fields.
   * @private
   */
  constructor() {
    super();
  }

  /**
   * @param {number} obj
   * @return {number}
   * @public
   * @nocollapse
   */
  static $create__double(obj) {
    Double.$clinit();
    return obj;
  }

  /**
   * @param {number} x
   * @param {number} y
   * @return {number}
   * @public
   * @nocollapse
   */
  static m_compareTo__double__double(x, y) {
    if (x < y) {
      return -1;
    } else if (x > y) {
      return 1;
    } else {
      return 0;
    }
  }

  /**
   * Returns whether the provided instance is an instance of this class.
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isInstance(instance) {
    return typeof instance == 'number';
  }

  /**
   * Returns whether the provided class is or extends this class.
   * @param {Function} classConstructor
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return $Util.$canCastClass(classConstructor, Double);
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    Double.$clinit();
    if (!Double.$classDouble_) {
      Double.$classDouble_ = Class.$createForClass(
          $Util.$generateId('Double'),
          $Util.$generateId('java.lang.Double'),
          Number.$getClass(),
          $Util.$generateId('java.lang.Double'));
    }
    return Double.$classDouble_;
  }

  /**
   * Runs inline static field initializers.
   * @protected
   * @nocollapse
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
    Number.$clinit();
  }
};


/**
 * The class literal field.
 * @private {Class}
 */
Double.$classDouble_ = null;


/**
 * Exported class.
 */
exports = Double;
