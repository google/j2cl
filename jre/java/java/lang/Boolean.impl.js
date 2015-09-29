/**
 * Impl super source for java.lang.Boolean.
 */
goog.module('gen.java.lang.Boolean$impl');


let Object = goog.require('gen.java.lang.Object$impl');
let $Util = goog.require('nativebootstrap.Util$impl');

let Class = goog.forwardDeclare('gen.java.lang.Class$impl');


class Boolean extends Object {
  /**
   * Defines instance fields.
   * @private
   */
  constructor() {
    super();
  }

  /**
   * @param {boolean} obj
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $create__boolean(obj) {
    Boolean.$clinit();
    return obj;
  }

  /**
   * @param {boolean} x
   * @param {boolean} y
   * @return {number}
   * @public
   * @nocollapse
   */
  static m_compareTo__boolean__boolean(x, y) {
    return (x === y) ? 0 : (x ? 1 : -1);
  }

  /**
   * Returns whether the provided instance is an instance of this class.
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isInstance(instance) {
    return typeof instance == 'boolean';
  }

  /**
   * Returns whether the provided class is or extends this class.
   * @param {Function} classConstructor
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return $Util.$canCastClass(classConstructor, Boolean);
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    Boolean.$clinit();
    if (!Boolean.$classBoolean_) {
      Boolean.$classBoolean_ = Class.$createForClass(
          $Util.$generateId('Boolean'),
          $Util.$generateId('java.lang.Boolean'),
          Object.$getClass(),
          $Util.$generateId('java.lang.Boolean'));
    }
    return Boolean.$classBoolean_;
  }

  /**
   * Runs inline static field initializers.
   * @protected
   * @nocollapse
   */
  static $clinit() {
    Class = goog.module.get('gen.java.lang.Class$impl');
    Object.$clinit();
  }
};


/**
 * The class literal field.
 * @private {Class}
 */
Boolean.$classBoolean_ = null;


/**
 * Exported class.
 */
exports = Boolean;
