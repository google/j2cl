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
    return classConstructor === $char;
  }

  /**
   * @return {Class}
   * @public
   */
  static $getClass() {
    $char.$clinit();
    if (!$char.$classChar_) {
      $char.$classChar_ = Class.$createForPrimitive('char');
    }
    return $char.$classChar_;
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
$char.$classChar_ = null;


/**
 * @public {number}
 */
$char.$initialArrayValue = 0;


/**
 * Exported class.
 */
exports = $char;
