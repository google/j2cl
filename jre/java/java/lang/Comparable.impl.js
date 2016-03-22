/**
 * Impl transpiled from java.lang.Comparable.
 */
goog.module('gen.java.lang.Comparable$impl');


let $Util = goog.require('nativebootstrap.Util$impl');

let Class = goog.forwardDeclare('gen.java.lang.Class$impl');


/**
 * @interface
 * @template C_T
 */
class Comparable {
  /**
   * @param {C_T} other
   * @return {number}
   * @public
   */
  m_compareTo__java_lang_Object(other) {
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
        .$implements__java_lang_Comparable = true;
  }

  /**
   * Returns whether the provided instance is of a class that implements this
   * interface.
   * @param {*} instance
   * @return {boolean}
   * @public
   */
  static $isInstance(instance) {
    let type = typeof instance;
    if (type == 'boolean' || type == 'number' || type == 'string') {
      return true;
    }
    return instance != null &&
           instance.$implements__java_lang_Comparable;
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
               .$implements__java_lang_Comparable;
  }

  /**
   * @return {Class}
   * @public
   */
  static $getClass() {
    Comparable.$clinit();
    if (!Comparable.$classComparable_) {
      Comparable.$classComparable_ = Class.$createForInterface(
          $Util.$generateId('Comparable'),
          $Util.$generateId('java.lang.Comparable'),
          $Util.$generateId('java.lang.Comparable'));
    }
    return Comparable.$classComparable_;
  }

  /**
   * Runs inline static field initializers.
   * @protected
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
  }};


/**
 * The class literal field.
 * @private {Class}
 */
Comparable.$classComparable_ = null;


Comparable.$markImplementor(/** @type {Function} */(Comparable));

/**
 * Export class.
 */
exports = Comparable;
