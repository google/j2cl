/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.primitives.$short$impl');


let Class = goog.forwardDeclare('gen.java.lang.Class$impl');


/**
 * Placeholder class definition for the primitive class short.
 *
 * Non-instantiable.
 */
class $short {
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
    return classConstructor === $short;
  }

  /**
   * @return {Class}
   * @public
   */
  static $getClass() {
    $short.$clinit();
    if (!$short.$classShort_) {
      $short.$classShort_ = Class.$createForPrimitive('short');
    }
    return $short.$classShort_;
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
$short.$classShort_ = null;


/**
 * @public {number}
 */
$short.$initialArrayValue = 0;


/**
 * Exported class.
 */
exports = $short;
