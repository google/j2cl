/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.primitives.$double$impl');


let Class = goog.forwardDeclare('gen.java.lang.Class$impl');


/**
 * Placeholder class definition for the primitive class double.
 *
 * Non-instantiable.
 */
class $double {
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
    return classConstructor === $double;
  }

  /**
   * @return {Class}
   * @public
   */
  static $getClass() {
    $double.$clinit();
    if (!$double.$classDouble_) {
      $double.$classDouble_ = Class.$createForPrimitive('double');
    }
    return $double.$classDouble_;
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
$double.$classDouble_ = null;


/**
 * @public {number}
 */
$double.$initialArrayValue = 0;


/**
 * Exported class.
 */
exports = $double;
