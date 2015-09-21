/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.primitives.$long$impl');


let Class = goog.forwardDeclare('gen.java.lang.Class$impl');


/**
 * Placeholder class definition for the primitive class long.
 *
 * Non-instantiable.
 */
class $long {
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
    return classConstructor === $long;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    $long.$clinit();
    if (!$long.$classLong) {
      $long.$classLong = Class.$createForPrimitive('long');
    }
    return $long.$classLong;
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
$long.$initialArrayValue = 0;


/**
 * Exported class.
 */
exports = $long;
