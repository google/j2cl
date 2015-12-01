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
   */
  static $isInstance(instance) { return typeof instance === 'number'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $int;
  }

  /**
   * @return {Class}
   * @public
   */
  static $getClass() {
    $int.$clinit();
    if (!$int.$classInt_) {
      $int.$classInt_ = Class.$createForPrimitive('int');
    }
    return $int.$classInt_;
  }

  /**
   * Runs inline static field initializers.
   * @protected
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
  }
};


/**
 * The class literal field.
 * @private {Class}
 */
$int.$classInt_ = null;


/**
 * @public {number}
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
