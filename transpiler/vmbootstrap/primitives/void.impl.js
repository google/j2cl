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
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isInstance(instance) { return false; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $void;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
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
   * @nocollapse
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
 * @nocollapse
 */
$void.$initialArrayValue = false;


/**
 * Exported class.
 */
exports = $void;
