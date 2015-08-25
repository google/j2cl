goog.module('gen.java.lang.DoubleModule');

/**
 * Transpiled from java/lang/Double.java.
 */
class Double {
  /**
   * @param {number} obj
   * @return {number}
   * @public
   * @nocollapse
   */
  static $create__double(obj) {
    return obj;
  }

  /**
   * Returns whether the provided instance is an instance of this class.
   * @return {boolean}
   * @public
   * @nocollapse
   */
  static $isInstance(instance) {
    return typeof instance == 'number';
  }
};


/**
 * Exported class.
 */
exports.Double = Double;
