/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.primitives.$float$impl');


let Class = goog.forwardDeclare('gen.java.lang.Class$impl');


/**
 * Placeholder class definition for the primitive class float.
 *
 * Non-instantiable.
 */
class $float {
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
    return classConstructor === $float;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    $float.$clinit();
    if (!$float.$classFloat) {
      $float.$classFloat = Class.$createForPrimitive('float');
    }
    return $float.$classFloat;
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
$float.$initialArrayValue = 0;


/**
 * Exported class.
 */
exports = $float;
