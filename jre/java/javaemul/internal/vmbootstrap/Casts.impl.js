/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Casts$impl');


let InternalPreconditions = goog.forwardDeclare('javaemul.internal.InternalPreconditions$impl');


class Casts {
  /**
   * @param {*} instance
   * @param {*} castType
   * @return {*}
   */
  static to(instance, castType) {
    return Casts.toInternal(
        instance, /** @type {Function} */ (castType.$isInstance));
  }

  /**
   * @param {*} instance
   * @param {Function} castTypeIsInstance
   * @return {*}
   */
  static toInternal(instance, castTypeIsInstance) {
    Casts.$clinit();

    // TODO(goktug) remove isTypeCheck after JsCompiler can remove calls to
    // castTypeIsInstance when the return is unused.
    if (InternalPreconditions.m_isTypeChecked()) {
      InternalPreconditions.m_checkType__boolean(
          instance == null || castTypeIsInstance(instance));
    }

    return instance;
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    InternalPreconditions =
        goog.module.get('javaemul.internal.InternalPreconditions$impl');
  }
};


/**
 * Exported class.
 */
exports = Casts;
