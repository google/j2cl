goog.module('gen.java.lang.StringModule');

var Class = goog.require('gen.java.lang.CoreModule').Class;
var Object = goog.require('gen.java.lang.CoreModule').Object;
var Util = goog.require('nativebootstrap.UtilModule').Util;

class String extends Object {
  // TODO: implement other String methods and constructors.
  /**
   * Returns whether the provided instance is of a class that implements this
   * interface.
   * @param {*} instance
   * @nocollapse
   */
  static $isInstance(instance) {
    return typeof instance == 'string';
  }

  /**
   * Returns whether the provided class is or extends this class.
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   * @nocollapse
   */
  static $isAssignableFrom(classConstructor) {
    return Util.$canCastClass(classConstructor, String);
  }

  /**
   * @return {Class}
   * @public
   * @nocollapse
   */
  static $getClass() {
    if (!String.$classString) {
      String.$classString = Class.$createForClass(
          Util.$generateId('String'),
          Util.$generateId('java.lang.String'),
          Object.$getClass(),
          Util.$generateId('java.lang.String'));
    }
    return String.$classString;
  }
};


/**
 * Exported class.
 */
exports.String = String;
