/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.primitives.$boolean$impl');


let Class = goog.forwardDeclare('gen.java.lang.Class$impl');


/**
 * Placeholder class definition for the primitive class boolean.
 *
 * Non-instantiable.
 */
class $boolean {
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
  static $isInstance(instance) { return typeof instance === 'boolean'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $boolean;
  }

  /**
   * @return {Class}
   * @public
   */
  static $getClass() {
    $boolean.$clinit();
    if (!$boolean.$classBoolean_) {
      $boolean.$classBoolean_ = Class.$createForPrimitive('boolean');
    }
    return $boolean.$classBoolean_;
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
$boolean.$classBoolean_ = null;


/**
 * @public {boolean}
 */
$boolean.$initialArrayValue = false;


/**
 * Exported class.
 */
exports = $boolean;
