/**
 * Impl transpiled from java.lang.CharSequence.
 */
goog.module('gen.java.lang.CharSequence$impl');


let $Util = goog.require('nativebootstrap.Util$impl');

let Class = goog.forwardDeclare('gen.java.lang.Class$impl');
let String = goog.forwardDeclare('gen.java.lang.String$impl');
let $int = goog.forwardDeclare('vmbootstrap.primitives.$int$impl');


/**
 * @interface
 */
class CharSequence {
  /**
   * @param {number} index
   * @return {number}
   * @public
   */
  m_charAt__int(index) {
  }

  /**
   * @return {number}
   * @public
   */
  m_length() {
  }

  /**
   * @param {number} start
   * @param {number} end
   * @return {(CharSequence|?string)}
   * @public
   */
  m_subSequence__int__int(start, end) {
  }

  /**
   * @return {?string}
   * @public
   */
  m_toString() {
  }

  /**
   * Marks the provided class as implementing this interface.
   * @param {Function} classConstructor
   * @public
   */
  static $markImplementor(classConstructor) {
    /**
     * @public {boolean}
     */
    classConstructor.prototype
        .$implements__java_lang_CharSequence = true;
  }

  /**
   * Returns whether the provided instance is of a class that implements this
   * interface.
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) {
    if ((typeof instance) == 'string') {
      return true;
    }
    return instance != null &&
           instance.$implements__java_lang_CharSequence;
  }

  /**
   * Returns whether the provided class is or extends this class.
   * @param {Function} classConstructor
   * @return {boolean}
   * @public
   */
  static $isAssignableFrom(classConstructor) {
    return classConstructor != null &&
           classConstructor.prototype
               .$implements__java_lang_CharSequence;
  }

  /**
   * @return {Class}
   * @public
   */
  static $getClass() {
    CharSequence.$clinit();
    if (!CharSequence.$classCharSequence_) {
      CharSequence.$classCharSequence_ = Class.$createForInterface(
          $Util.$generateId('CharSequence'),
          $Util.$generateId('java.lang.CharSequence'), null,
          $Util.$generateId('java.lang.CharSequence'));
    }
    return CharSequence.$classCharSequence_;
  }

  /**
   * Runs inline static field initializers.
   * @protected
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
    String = goog.module.get('gen.java.lang.String$impl');
    $int = goog.module.get('vmbootstrap.primitives.$int$impl');
  }};


/**
 * The class literal field.
 * @private {Class}
 */
CharSequence.$classCharSequence_ = null;


CharSequence.$markImplementor(/** @type {Function} */(CharSequence));

/**
 * Export class.
 */
exports = CharSequence;
