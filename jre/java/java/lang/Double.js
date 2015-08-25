goog.module('gen.java.lang.DoubleModule');


var Class = goog.require('gen.java.lang.CoreModule').Class;
var Number = goog.require('gen.java.lang.NumberModule').Number;
var $Util = goog.require('nativebootstrap.UtilModule').Util;


/**
 * Super-sourcing java/lang/Double.java.
 */
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
    return obj;
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
};


/**
 * @public {Class}
 * @nocollapse
 */
Double.$class = Class.$createForClass(
  $Util.$generateId('Double'),
  $Util.$generateId('java.lang.Double'),
  Number.$class,
  $Util.$generateId('java.lang.Double'));

/**
 * Exported class.
 */
exports.Double = Double;
