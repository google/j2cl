/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Casts$impl');


let Class = goog.forwardDeclare('java.lang.Class$impl');
let InternalPreconditions = goog.forwardDeclare('javaemul.internal.InternalPreconditions$impl');
let Objects = goog.forwardDeclare('vmbootstrap.Objects$impl');


class Casts {
  /**
   * @param {*} instance
   * @param {*} castType
   * @return {*}
   */
  static to(instance, castType) {
    return Casts.toInternal(
        instance, /** @type {Function} */ (castType.$isInstance), castType);
  }

  /**
   * @param {*} instance
   * @param {Function} castTypeIsInstance
   * @param {*} castType
   * @return {*}
   */
  static toInternal(instance, castTypeIsInstance, castType) {
    Casts.$clinit();

    // TODO(goktug) remove isTypeCheck after JsCompiler can remove calls to
    // castTypeIsInstance when the return is unused.
    if (InternalPreconditions.m_isTypeChecked__()) {
      var castSucceeds = instance == null || castTypeIsInstance(instance);
      if (!castSucceeds) {
        Casts.throwClassCastException(instance, castType);
      }
    }

    return instance;
  }

  /**
   * @param {*} instance
   * @param {*} castType
   * @param {number=} opt_dimensionCount
   */
  static throwClassCastException(instance, castType, opt_dimensionCount) {
    Casts.$clinit();

    var castTypeClass = Class.$get(castType, opt_dimensionCount);
    var instanceTypeClass = Objects.m_getClass__java_lang_Object(instance);
    var message = instanceTypeClass.m_getName__() + ' cannot be cast to ' +
        castTypeClass.m_getName__();
    InternalPreconditions.m_checkType__boolean__java_lang_String(
        false, message);
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    Class = goog.module.get('java.lang.Class$impl');
    InternalPreconditions =
        goog.module.get('javaemul.internal.InternalPreconditions$impl');
    Objects = goog.module.get('vmbootstrap.Objects$impl');
  }
};


/**
 * Exported class.
 */
exports = Casts;
