goog.module('vmbootstrap.ArraysModule');


var Serializable = goog.require('gen.java.io.SerializableModule').Serializable;
var ArrayIndexOutOfBoundsException = goog.require(
  'gen.java.lang.ArrayIndexOutOfBoundsExceptionModule'
).ArrayIndexOutOfBoundsException;
var ArrayStoreException = goog.require(
  'gen.java.lang.ArrayStoreExceptionModule').ArrayStoreException;
var Cloneable = goog.require('gen.java.lang.CloneableModule').Cloneable;
var Class = goog.require('gen.java.lang.CoreModule').Class;
var Object = goog.require('gen.java.lang.CoreModule').Object;
var NullPointerException = goog.require(
  'gen.java.lang.NullPointerExceptionModule').NullPointerException;
var $boolean = goog.require('vmbootstrap.PrimitivesModule').$boolean;
var $byte = goog.require('vmbootstrap.PrimitivesModule').$byte;
var $char = goog.require('vmbootstrap.PrimitivesModule').$char;
var $double = goog.require('vmbootstrap.PrimitivesModule').$double;
var $float = goog.require('vmbootstrap.PrimitivesModule').$float;
var $int = goog.require('vmbootstrap.PrimitivesModule').$int;
var $long = goog.require('vmbootstrap.PrimitivesModule').$long;
var $short = goog.require('vmbootstrap.PrimitivesModule').$short;


// java.lang.Object/Serializable/Cloneable cast check functions contain special
// case handling for arrays so that the Array.prototype can be left clean and so
// that java.lang.Object is not forced to be the same class as JS's Object root.


/**
 * Static Array helper functions.
 *
 * @public
 */
class Arrays {
  /**
   * Creates, initializes, and returns an array with the given number of
   * dimensions, lengths and of the given type.
   *
   * @param {Array<number>} dimensionLengths
   * @param {Function} leafType
   * @return {Array<?>}
   * @public
   */
  static $create(dimensionLengths, leafType) {
    var length = dimensionLengths[0];
    if (length == null) {
      return null;
    }

    var array = new Array;
    array.leafType = leafType;
    array.dimensionCount = dimensionLengths.length;

    array.$class = Class.$createForArray(leafType.$class, array.dimensionCount);

    array.length = length;
    if (array.dimensionCount > 1) {
      var subDimensionLengths = dimensionLengths.slice(1);
      for (var i = 0; i < length; i++) {
        array[i] = Arrays.$create(subDimensionLengths, leafType);
      }
    }

    return array;
  }

  /**
   * Returns the given array after marking it with known # of dimensions and
   * leafType.
   * <p>
   * Dimension count is optional and omission indicates 1 dimension.
   * <p>
   * Unlike array creation, the actual lengths of each dimension do not need to
   * be specified because the passed array already contains values.
   * <p>
   * This modification is potentially destructive and should only ever be
   * applied to brand new array literals.
   *
   * @param {Array<?>} array
   * @param {Function} leafType
   * @param {number} opt_dimensionCount
   * @return {Array<?>}
   * @public
   */
  static $init(array, leafType, opt_dimensionCount) {
    opt_dimensionCount = opt_dimensionCount || 1;
    Arrays.$initRecursive(array, leafType, opt_dimensionCount);
    return array;
  }

  /**
   * Recursively marks the given array and any contained arrays with known
   * dimensions and leafType.
   *
   * @param {Array<?>} array
   * @param {Function} leafType
   * @param {number} dimensionCount
   * @private
   */
  static $initRecursive(array, leafType, dimensionCount) {
    array.leafType = leafType;
    array.dimensionCount = dimensionCount;

    array.$class = Class.$createForArray(leafType.$class, array.dimensionCount);

    // Length is not set since the provided array already contain values and
    // knows its own length.
    if (array.dimensionCount > 1) {
      for (var i = 0; i < array.length; i++) {
        var nestedArray = array[i];
        if (nestedArray == null) {
          continue;
        }
        Arrays.$initRecursive(nestedArray, leafType, dimensionCount - 1);
      }
    }
  }

  /**
   * Sets the given value into the given index in the given array.
   *
   * @param {Array<Object>} array
   * @param {number} index
   * @param {Object} value
   * @public
   */
  static $set(array, index, value) {
    if (ARRAY_CHECKS_ENABLED_) {
      if (array == null) {
        // Array can not be null.
        Arrays.$throwNullPointerException();
      }
      if (index >= array.length || index < 0) {
        // Index must be within bounds.
        Arrays.$throwArrayIndexOutOfBoundsException();
      }
      // Only check when the array has a known leaf type. JS native arrays won't
      // have it.
      if (array.leafType != null) {
        if (array.dimensionCount > 1) {
          if (!Arrays.$instanceIsOfType(
                  value, array.leafType, array.dimensionCount - 1)) {
            // The inserted array must fit dimensions and the array leaf
            // type.
            Arrays.$throwArrayStoreException();
          }
        } else if (!array.leafType.$isInstance(value)) {
          // The inserted value must fit the array leaf type.
          Arrays.$throwArrayStoreException();
        }
      }
    }

    array[index] = value;
  }

  /**
   * Returns whether the given instance is an array, whether it has the given
   * number of dimensions and whether its leaf type is assignable from the given
   * leaf type.
   *
   * @param {?} instance
   * @param {Function} requiredLeafType
   * @param {number} requiredDimensionCount
   * @return {boolean}
   * @public
   */
  static $instanceIsOfType(instance, requiredLeafType, requiredDimensionCount) {
    if (instance == null || !Array.isArray(instance)) {
      // Null or not an Array can't cast.
      return false;
    }
    if (instance.dimensionCount == requiredDimensionCount) {
      // If dimensions are equal then the leaftypes must be castable.
      return requiredLeafType.$isAssignableFrom(instance.leafType);
    }
    if (instance.dimensionCount > requiredDimensionCount) {
      // If shrinking the dimensions then the new leaf type must be object.
      return Object.$isAssignableFrom(requiredLeafType);
    }
    return false;
  }

  /**
   * Isolates the exception throw here so that calling functions that perform
   * casts can still be optimized by V8.
   *
   * @private
   */
  static $throwArrayIndexOutOfBoundsException() {
    throw ArrayIndexOutOfBoundsException.$create();
  }

  /**
   * Isolates the exception throw here so that calling functions that perform
   * casts can still be optimized by V8.
   *
   * @private
   */
  static $throwArrayStoreException() {
    throw ArrayStoreException.$create();
  }

  /**
   * Isolates the exception throw here so that calling functions that perform
   * casts can still be optimized by V8.
   *
   * @private
   */
  static $throwNullPointerException() {
    throw NullPointerException.$create();
  }

  /**
   * Creates, initializes, and returns a byte array with the given number of
   * dimensions and lengths.
   *
   * @param {Array<number>} dimensionLengths
   * @return {Array<?>}
   * @public
   */
  static $createByte(dimensionLengths) {
    return Arrays.$create(dimensionLengths, $byte);
  }

  /**
   * Creates, initializes, and returns a short array with the given number of
   * dimensions and lengths.
   *
   * @param {Array<number>} dimensionLengths
   * @return {Array<?>}
   * @public
   */
  static $createShort(dimensionLengths) {
    return Arrays.$create(dimensionLengths, $short);
  }

  /**
   * Creates, initializes, and returns a int array with the given number of
   * dimensions and lengths.
   *
   * @param {Array<number>} dimensionLengths
   * @return {Array<?>}
   * @public
   */
  static $createInt(dimensionLengths) {
    return Arrays.$create(dimensionLengths, $int);
  }

  /**
   * Creates, initializes, and returns a long array with the given number of
   * dimensions and lengths.
   *
   * @param {Array<number>} dimensionLengths
   * @return {Array<?>}
   * @public
   */
  static $createLong(dimensionLengths) {
    return Arrays.$create(dimensionLengths, $long);
  }

  /**
   * Creates, initializes, and returns a float array with the given number of
   * dimensions and lengths.
   *
   * @param {Array<number>} dimensionLengths
   * @return {Array<?>}
   * @public
   */
  static $createFloat(dimensionLengths) {
    return Arrays.$create(dimensionLengths, $float);
  }

  /**
   * Creates, initializes, and returns a double array with the given number of
   * dimensions and lengths.
   *
   * @param {Array<number>} dimensionLengths
   * @return {Array<?>}
   * @public
   */
  static $createDouble(dimensionLengths) {
    return Arrays.$create(dimensionLengths, $double);
  }

  /**
   * Creates, initializes, and returns a char array with the given number of
   * dimensions and lengths.
   *
   * @param {Array<number>} dimensionLengths
   * @return {Array<?>}
   * @public
   */
  static $createChar(dimensionLengths) {
    return Arrays.$create(dimensionLengths, $char);
  }

  /**
   * Creates, initializes, and returns a boolean array with the given number of
   * dimensions and lengths.
   *
   * @param {Array<number>} dimensionLengths
   * @return {Array<?>}
   * @public
   */
  static $createBoolean(dimensionLengths) {
    return Arrays.$create(dimensionLengths, $boolean);
  }
};


/**
 * @define {boolean} Whether or not to check type and bounds on insertion.
 * @private
 */
goog.define('ARRAY_CHECKS_ENABLED_', true);


/**
 * Exported class.
 */
exports.Arrays = Arrays;
