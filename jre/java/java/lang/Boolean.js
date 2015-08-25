goog.module('gen.java.lang.BooleanModule');

/**
 * Transpiled from java/lang/Boolean.java.
 */
class Boolean {
  /**
   * @param {boolean} obj
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $create__boolean(obj) {
    return obj;
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
};


/**
 * Exported class.
 */
exports.Boolean = Boolean;
