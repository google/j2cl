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
   * @param {*} instance
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
    return classConstructor === $float;
  }

  /**
   * @return {Class}
   * @public
   */
  static $getClass() {
    $float.$clinit();
    if (!$float.$classFloat_) {
      $float.$classFloat_ = Class.$createForPrimitive('float');
    }
    return $float.$classFloat_;
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
$float.$classFloat_ = null;


/**
 * @public {number}
 */
$float.$initialArrayValue = 0;


/**
 * Exported class.
 */
exports = $float;
