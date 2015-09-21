/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.primitives.$int$impl');


let Class = goog.forwardDeclare('gen.java.lang.Class$impl');


/**
 * Placeholder class definition for the primitive class int.
 *
 * Non-instantiable.
 */
class $int {
  /**
   * Defines instance fields.
   *
   * @private
   */
  constructor() {}

  /**
   * Returns whether the provided instance is an instance of this class.
   *
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $int;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    $int.$clinit();
    if (!$int.$classInt) {
      $int.$classInt = Class.$createForPrimitive('int');
    }
    return $int.$classInt;
  }

  /**
   * Runs inline static field initializers.
   * @protected
   * @nocollapse
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
  }
};


/**
 * @public {number}
 * @nocollapse
 */
$int.$initialArrayValue = 0;


/**
 * @public {number}
 */
$int.MAX_VALUE = 0x7fffffff;


/**
 * @public {number}
 */
$int.MIN_VALUE = -0x80000000;


/**
 * Exported class.
 */
exports = $int;
