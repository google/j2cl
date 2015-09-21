/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.primitives.$char$impl');


let Class = goog.forwardDeclare('gen.java.lang.Class$impl');


/**
 * Placeholder class definition for the primitive class char.
 *
 * Non-instantiable.
 */
class $char {
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
    return classConstructor === $char;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    $char.$clinit();
    if (!$char.$classChar) {
      $char.$classChar = Class.$createForPrimitive('char');
    }
    return $char.$classChar;
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
$char.$initialArrayValue = 0;


/**
 * Exported class.
 */
exports = $char;
