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
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isInstance(instance) { return typeof instance === 'boolean'; }

  /**
   * Returns whether the provided class is or extends this class.
   *
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor === $boolean;
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
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
$boolean.$classBoolean_ = null;


/**
 * @public {boolean}
 * @nocollapse
 */
$boolean.$initialArrayValue = false;


/**
 * Exported class.
 */
exports = $boolean;
