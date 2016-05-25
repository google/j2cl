/**
 * Impl hand rolled.
 */
goog.module('nativebootstrap.Util$impl');


/**
 * Miscellaneous utility functions.
 */
class Util {
  /**
   * Return the value defined by a goog.define or the default value
   * if it is not defined.
   *
   * @param {string} name
   * @param {?string=} opt_defaultValue
   * @return {?string}
   * @public
   */
  static $getDefine(name, opt_defaultValue) {
    // Default the optional param. Note that we are not using the common
    // 'opt_value || default_value' pattern otherwise that would replace
    // empty string with null value.
    var defaultValue = opt_defaultValue == null ? null : opt_defaultValue;
    var rv = goog.getObjectByName(name);
    return rv == null ? defaultValue : String(rv);
  }

  /**
   * @param {*} ctor
   * @param {string} name
   * @public
   */
  static $setClassMetadata(ctor, name) {
    ctor.prototype.$$classMetadata = [name, Util.TYPE_CLASS];
  }

  /**
   * @param {*} ctor
   * @param {string} name
   * @public
   */
  static $setClassMetadataForInterface(ctor, name) {
    ctor.prototype.$$classMetadata = [name, Util.TYPE_INTERFACE];
  }

  /**
   * @param {*} ctor
   * @param {string} name
   * @public
   */
  static $setClassMetadataForEnum(ctor, name) {
    ctor.prototype.$$classMetadata = [name, Util.TYPE_ENUM];
  }

  /**
   * @param {*} ctor
   * @param {string} name
   * @public
   */
  static $setClassMetadataForPrimitive(ctor, name) {
    ctor.prototype.$$classMetadata = [name, Util.TYPE_PRIMITIVE];
  }

  /**
   * @param {*} ctor
   * @return {string}
   * @public
   */
  static $extractClassName(ctor) {
    if (CLASS_METADATA_ENABLED_) {
      return ctor.prototype.$$classMetadata[0];
    } else {
      // TODO(goktug): use uniq ID
      return 'Class$obf';
    }
  }

  /**
   * @param {*} ctor
   * @return {number}
   * @public
   */
  static $extractClassType(ctor) {
    if (CLASS_METADATA_ENABLED_) {
      return ctor.prototype.$$classMetadata[1];
    } else {
      return Util.TYPE_CLASS;
    }
  }

  /**
   * Returns whether the "from" class can be cast to the "to" class.
   *
   * Unlike instanceof, this function operates on classes instead of
   * instances.
   * @param {Function} fromClass
   * @param {Function} toClass
   * @return {boolean}
   * @public
   */
  static $canCastClass(fromClass, toClass) {
    return (fromClass != null &&
        (fromClass == toClass || fromClass.prototype instanceof toClass));
  }

  /**
   * Create a function that applies the specified jsFunctionMethod on itself,
   * and copies {@code instance}' properties to itself.
   *
   * @param {Function} jsFunctionMethod
   * @param {*} instance
   * @param {Function} copyMethod
   * @return {!Function}
   * @public
   */
  static $makeLambdaFunction(jsFunctionMethod, instance, copyMethod) {
    var lambda = function() {
      return jsFunctionMethod.apply(lambda, arguments);
    };
    copyMethod(instance, lambda);
    return lambda;
  }

  /**
   * Helper to access a field on the prototype since we don't current have a
   * way of representing this in our AST.
   * @param {Function} constructor
   * @return {*}
   * @public
   */
  static $getPrototype(constructor) { return constructor.prototype; }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {}
}


/**
 * Used to store qualifier that is potentially side effecting.
 * @public {*}
 */
Util.$q = null;


/**
 * @define {boolean} Whether or not to keep getName() and getCanonicalName()
 *         accurate.
 * @private
 */
goog.define('CLASS_METADATA_ENABLED_', true);


/**
 * @type {number}
 */
Util.TYPE_CLASS = 0;

/**
 * @type {number}
 */
Util.TYPE_INTERFACE = 1;

/**
 * @type {number}
 */
Util.TYPE_ENUM = 2;

/**
 * @type {number}
 */
Util.TYPE_PRIMITIVE = 3;


/**
 * Exported class.
 */
exports = Util;
