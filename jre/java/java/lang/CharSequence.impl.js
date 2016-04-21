/**
 * Impl transpiled from java.lang.CharSequence.
 */
goog.module('java.lang.CharSequence$impl');


let $Util = goog.require('nativebootstrap.Util$impl');

let String = goog.forwardDeclare('java.lang.String$impl');
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
   * @param {*} instance
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
   * Runs inline static field initializers.
   * @protected
   */
  static $clinit() {
    String = goog.module.get('java.lang.String$impl');
    $int = goog.module.get('vmbootstrap.primitives.$int$impl');
  }
};


$Util.$setClassMetadataForInterface(CharSequence, 'java.lang.CharSequence');


CharSequence.$markImplementor(/** @type {Function} */(CharSequence));

/**
 * Export class.
 */
exports = CharSequence;
