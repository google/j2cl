goog.module('gen.java.lang.StringModule');

var Class = goog.require('gen.java.lang.CoreModule').Class;
var Object = goog.require('gen.java.lang.CoreModule').Object;
var Util = goog.require('nativebootstrap.UtilModule').Util;

class String extends Object {
  // TODO: implement other String methods and constructors.
  /**
   * Returns whether the provided instance is of a class that implements this
   * interface.
   * @param {Object} instance
   */
  static $isInstance(instance) {
    return typeof instance == 'string';
  }

  /**
   * Returns whether the provided class is or extends this class.
   * @param {Function} classConstructor
   * @return {boolean}
   * @private
   */
  static $isAssignableFrom(classConstructor) {
    return Util.$canCastClass(classConstructor, String);
  }
};


/**
 * @public {Class}
 */
String.$class = Class.$createForClass(
    Util.$generateId('String'),
    Util.$generateId(
        'java.lang.String'),
    Object.$class,
    Util.$generateId(
        'java.lang.String'));


/**
 * Exported class.
 */
exports.String = String;
