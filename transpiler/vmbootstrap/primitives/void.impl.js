/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.primitives.$void$impl');


let Class = goog.forwardDeclare('gen.java.lang.Class$impl');


/**
 * Placeholder class definition for the primitive class void.
 *
 * Non-instantiable.
 */
class $void {
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
  static $isInstance(instance) { return false; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $void;
  }

  /**
   * @return {Class}
   * @public
   */
  static $getClass() {
    $void.$clinit();
    if (!$void.$classVoid_) {
      $void.$classVoid_ = Class.$createForPrimitive('void');
    }
    return $void.$classVoid_;
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
$void.$classVoid_ = null;


/**
 * @public {boolean}
 */
$void.$initialArrayValue = false;


/**
 * Exported class.
 */
exports = $void;
