/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Arrays$impl');


// Don't reformat these imports! The uncompiled test harness contains a bug
// that will miss some multiline goog.require's.
let Hashing = goog.require('nativebootstrap.Hashing$impl');

let ArrayIndexOutOfBoundsException =
    goog.forwardDeclare('gen.java.lang.ArrayIndexOutOfBoundsException$impl');
let ArrayStoreException =
    goog.forwardDeclare('gen.java.lang.ArrayStoreException$impl');
let Class = goog.forwardDeclare('gen.java.lang.Class');
let Object = goog.forwardDeclare('gen.java.lang.Object');
let Integer = goog.forwardDeclare('gen.java.lang.Integer$impl');
let NullPointerException =
    goog.forwardDeclare('gen.java.lang.NullPointerException$impl');
let Casts = goog.forwardDeclare('vmbootstrap.Casts$impl');
let Exceptions = goog.forwardDeclare('vmbootstrap.Exceptions$impl');


/**
 * Static Array helper and devirtualized functions.
 *
 * @public
 */
class Arrays {
  /**
   * Creates, initializes, and returns an array with the given number of
   * dimensions, lengths and of the given type.
   *
   * @param {Array<number>} dimensionLengths
   * @param {*} leafType
   * @return {Array<*>}
   * @public
   */
  static $create(dimensionLengths, leafType) {
    return Arrays.$createInternal(
        dimensionLengths, leafType, leafType.$isInstance,
        leafType.$isAssignableFrom, leafType.$getClass,
        leafType.$initialArrayValue);
  }

  /**
   * @param {Array<number>} dimensionLengths
   * @param {*} leafType
   * @param {Function} leafTypeIsInstance
   * @param {Function} leafTypeIsAssignableFrom
   * @param {Function} leafTypeGetClass
   * @return {Array<*>}
   * @private
   */
  static $createInternal(
      dimensionLengths, leafType, leafTypeIsInstance, leafTypeIsAssignableFrom,
      leafTypeGetClass, leafTypeInitialValue) {
    let length = dimensionLengths[0];
    if (length == null) {
      return null;
    }

    let array = [];
    array.leafType = leafType;
    array.leafTypeIsInstance = leafTypeIsInstance;
    array.leafTypeIsAssignableFrom = leafTypeIsAssignableFrom;
    array.leafTypeGetClass = leafTypeGetClass;
    array.dimensionCount = dimensionLengths.length;
    array.length = length;

    if (array.dimensionCount > 1) {
      // Contains sub arrays.
      let subDimensionLengths = dimensionLengths.slice(1);
      for (let i = 0; i < length; i++) {
        array[i] = Arrays.$createInternal(
            subDimensionLengths, leafType, leafTypeIsInstance,
            leafTypeIsAssignableFrom, leafTypeGetClass, leafTypeInitialValue);
      }
    } else {
      // Contains leaf type values.
      if (leafTypeInitialValue !== undefined) {
        // Replace with Array.fill() when there is broad browser support.
        for (let index = 0; index < length; index++) {
          array[index] = leafTypeInitialValue;
        }
      } else {
        // Object leaf types don't need a defined initial value because the
        // Javascript array's default of 'undefined' works fine.
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
   * @param {Array<*>} array
   * @param {*} leafType
   * @param {number} opt_dimensionCount
   * @return {Array<*>}
   * @public
   */
  static $init(array, leafType, opt_dimensionCount) {
    return Arrays.$initRecursiveInternal(
        array, leafType, leafType.$getClass, leafType.$isInstance,
        leafType.$isAssignableFrom, opt_dimensionCount || 1);
  }

  /**
   * @param {Array<*>} array
   * @param {*} leafType
   * @param {Function} leafTypeGetClass
   * @param {Function} leafTypeIsInstance
   * @param {Function} leafTypeIsAssignableFrom
   * @param {number} dimensionCount
   * @return {Array<*>}
   * @private
   */
  static $initRecursiveInternal(
      array, leafType, leafTypeGetClass, leafTypeIsInstance,
      leafTypeIsAssignableFrom, dimensionCount) {
    array.leafType = leafType;
    array.leafTypeIsInstance = leafTypeIsInstance;
    array.leafTypeIsAssignableFrom = leafTypeIsAssignableFrom;
    array.leafTypeGetClass = leafTypeGetClass;
    array.dimensionCount = dimensionCount;

    // Length is not set since the provided array already contain values and
    // knows its own length.
    if (array.dimensionCount > 1) {
      for (let i = 0; i < array.length; i++) {
        let nestedArray = array[i];
        if (nestedArray == null) {
          continue;
        }
        Arrays.$initRecursiveInternal(
            /** @type {Array<*>} */ (nestedArray), leafType, leafTypeGetClass,
            leafTypeIsInstance, leafTypeIsAssignableFrom, dimensionCount - 1);
      }
    }

    return array;
  }

  /**
   * Sets the given value into the given index in the given array.
   *
   * @template T
   * @param {Array<*>} array
   * @param {number} index
   * @param {T} value
   * @return {T}
   * @public
   */
  static $set(array, index, value) {
    Arrays.$clinit();
    if (array == null) {
      // Array can not be null.
      Arrays.$throwNullPointerException();
    }
    if (ARRAY_CHECK_BOUNDS_) {
      if (index >= array.length || index < 0) {
        // Index must be within bounds.
        Arrays.$throwArrayIndexOutOfBoundsException();
      }
    }
    if (ARRAY_CHECK_TYPES_) {
      // Only check when the array has a known leaf type. JS native arrays won't
      // have it.
      if (array.leafTypeIsInstance != null) {
        if (array.dimensionCount > 1) {
          if (!Arrays.$instanceIsOfTypeInternal(
                  value, array.leafType, array.leafTypeIsAssignableFrom,
                  array.dimensionCount - 1)) {
            // The inserted array must fit dimensions and the array leaf
            // type.
            Arrays.$throwArrayStoreException();
          }
        } else if (value != null && !array.leafTypeIsInstance(value)) {
          // The inserted value must fit the array leaf type.
          // If leafType is not a primitive type, a 'null' should always be a
          // legal value. If leafType is a primitive type, value cannot be null
          // because that is illegal in Java.
          Arrays.$throwArrayStoreException();
        }
      }
    }

    return array[index] = value;
  }

  /**
   * Changes the given array's to the given leafType.
   *
   * @param {Array<*>} array
   * @param {Array<*>} otherArray
   * @public
   */
  static $stampType(array, otherArray) {
    array.leafType = otherArray.leafType;
    array.leafTypeIsInstance = otherArray.leafTypeIsInstance;
    array.leafTypeIsAssignableFrom = otherArray.leafTypeIsAssignableFrom;
    array.leafTypeGetClass = otherArray.leafTypeGetClass;
    array.dimensionCount = otherArray.dimensionCount;
  }

  /**
   * Returns whether the given instance is an array, whether it has the given
   * number of dimensions and whether its leaf type is assignable from the given
   * leaf type.
   *
   * @param {*} instance
   * @param {*} requiredLeafType
   * @param {number} requiredDimensionCount
   * @return {boolean}
   * @public
   */
  static $instanceIsOfType(instance, requiredLeafType, requiredDimensionCount) {
    return Arrays.$instanceIsOfTypeInternal(
        instance, requiredLeafType, requiredLeafType.$isAssignableFrom,
        requiredDimensionCount);
  }

  /**
   * @param {*} instance
   * @param {*} requiredLeafType
   * @param {Function} requiredLeafTypeIsAssignableFrom
   * @param {number} requiredDimensionCount
   * @return {boolean}
   * @private
   */
  static $instanceIsOfTypeInternal(
      instance, requiredLeafType, requiredLeafTypeIsAssignableFrom,
      requiredDimensionCount) {
    Arrays.$clinit();
    if (instance == null || !Array.isArray(instance)) {
      // Null or not an Array can't cast.
      return false;
    }

    // One dimensional Object arrays are emitted as a raw JS [] array literal
    // and will be missing the dimensionCount field.
    var effectiveInstanceDimensionCount = (instance.dimensionCount || 1);

    if (effectiveInstanceDimensionCount == requiredDimensionCount) {
      // If dimensions are equal then the leaftypes must be castable.
      return requiredLeafTypeIsAssignableFrom(instance.leafType);
    }
    if (effectiveInstanceDimensionCount > requiredDimensionCount) {
      // If shrinking the dimensions then the new leaf type must *be* Object.
      return Object == requiredLeafType;
    }
    return false;
  }

  /**
   * Returns whether the given instance is a raw JS array.
   *
   * @param {*} instance
   * @return {boolean}
   * @public
   */
  static $instanceIsOfNative(instance) { return Array.isArray(instance); }

  /**
   * Casts the provided instance to the provided array type.
   * <p>
   * If the cast is invalid then an exception will be thrown otherwise the
   * provided instance is returned.
   *
   * @param {*} instance
   * @param {*} requiredLeafType
   * @param {number} requiredDimensionCount
   * @return {*}
   * @public
   */
  static $castTo(instance, requiredLeafType, requiredDimensionCount) {
    return Arrays.$castToInternal(
        instance, requiredLeafType, requiredLeafType.$isAssignableFrom,
        requiredDimensionCount);
  }

  /**
   * @param {*} instance
   * @param {*} requiredLeafType
   * @param {number} requiredDimensionCount
   * @return {*}
   * @public
   */
  static $castToInternal(
      instance, requiredLeafType, requiredLeafTypeIsAssignableFrom,
      requiredDimensionCount) {
    Arrays.$clinit();
    if (instance == null) {
      return instance;
    }
    return Casts.check(
        instance,
        Arrays.$instanceIsOfTypeInternal(
            instance, requiredLeafType, requiredLeafTypeIsAssignableFrom,
            requiredDimensionCount));
  }

  /**
   * Casts the provided instance to a raw JS array type. It is valid if the
   * instance is a JS array.
   * <p>
   * If the cast is invalid then an exception will be thrown otherwise the
   * provided instance is returned.
   *
   * @param {*} instance
   * @return {*}
   * @public
   */
  static $castToNative(instance) {
    Arrays.$clinit();
    if (instance == null) {
      return instance;
    }
    return Casts.check(instance, Array.isArray(instance));
  }

  /**
   * @param {*} obj
   * @param {*} other
   * @return {boolean}
   * @public
   */
  static m_equals__java_lang_Object__java_lang_Object(obj, other) {
    return obj === other;
  }

  /**
   * @param {*} obj
   * @return {number}
   * @public
   */
  static m_hashCode__java_lang_Object(obj) {
    return Hashing.$getHashCode(/** @type {Object} */ (obj));
  }

  /**
   * @param {*} obj
   * @return {?string}
   * @public
   */
  static m_toString__java_lang_Object(obj) {
    Arrays.$clinit();
    return Arrays.m_getClass__java_lang_Object(obj).m_getName() + '@' +
           Integer.m_toHexString__int(Hashing.$getHashCode(obj));
  }

  /**
   * @param {*} obj
   * @return {Class}
   * @public
   */
  static m_getClass__java_lang_Object(obj) {
    Arrays.$clinit();
    if (obj.leafTypeGetClass) {
      return obj.leafTypeGetClass().$forArray(obj.dimensionCount || 1);
    }
    // Uninitialized arrays lack a 'leafTypeGetClass' method but are implicitly
    // Object[].
    return Object.$getClass().$forArray(1);
  }

  /**
   * Isolates the exception throw here so that calling functions that perform
   * casts can still be optimized by V8.
   *
   * @private
   */
  static $throwArrayIndexOutOfBoundsException() {
    Arrays.$clinit();
    throw Exceptions.toJs(ArrayIndexOutOfBoundsException.$create());
  }

  /**
   * Isolates the exception throw here so that calling functions that perform
   * casts can still be optimized by V8.
   *
   * @private
   */
  static $throwArrayStoreException() {
    Arrays.$clinit();
    throw Exceptions.toJs(ArrayStoreException.$create());
  }

  /**
   * Isolates the exception throw here so that calling functions that perform
   * casts can still be optimized by V8.
   *
   * @private
   */
  static $throwNullPointerException() {
    Arrays.$clinit();
    throw Exceptions.toJs(NullPointerException.$create());
  }

  /**
   * @param {Array<number>} array
   * @param {number} index
   * @param {number} value
   * @public
   */
  static $addSet(array, index, value) {
    // TODO: apply array.leafType.narrow() after computation.
    Arrays.$set(array, index, array[index] + value);
  }

  /**
   * @param {Array<number>} array
   * @param {number} index
   * @param {number} value
   * @public
   */
  static $subSet(array, index, value) {
    // TODO: apply array.leafType.narrow() after computation.
    Arrays.$set(array, index, array[index] - value);
  }

  /**
   * @param {Array<number>} array
   * @param {number} index
   * @param {number} value
   * @public
   */
  static $mulSet(array, index, value) {
    // TODO: apply array.leafType.narrow() after computation.
    Arrays.$set(array, index, array[index] * value);
  }

  /**
   * @param {Array<number>} array
   * @param {number} index
   * @param {number} value
   * @public
   */
  static $divSet(array, index, value) {
    // TODO: apply array.leafType.narrow() after computation.
    Arrays.$set(array, index, array[index] / value);
  }

  /**
   * @param {Array<number>} array
   * @param {number} index
   * @param {number} value
   * @public
   */
  static $andSet(array, index, value) {
    // TODO: apply array.leafType.narrow() after computation.
    Arrays.$set(array, index, array[index] & value);
  }

  /**
   * @param {Array<number>} array
   * @param {number} index
   * @param {number} value
   * @public
   */
  static $orSet(array, index, value) {
    // TODO: apply array.leafType.narrow() after computation.
    Arrays.$set(array, index, array[index] | value);
  }

  /**
   * @param {Array<number>} array
   * @param {number} index
   * @param {number} value
   * @public
   */
  static $xorSet(array, index, value) {
    // TODO: apply array.leafType.narrow() after computation.
    Arrays.$set(array, index, array[index] ^ value);
  }

  /**
   * @param {Array<number>} array
   * @param {number} index
   * @param {number} value
   * @public
   */
  static $modSet(array, index, value) {
    // TODO: apply array.leafType.narrow() after computation.
    Arrays.$set(array, index, array[index] % value);
  }

  /**
   * @param {Array<number>} array
   * @param {number} index
   * @param {number} value
   * @public
   */
  static $lshiftSet(array, index, value) {
    // TODO: apply array.leafType.narrow() after computation.
    Arrays.$set(array, index, array[index] << value);
  }

  /**
   * @param {Array<number>} array
   * @param {number} index
   * @param {number} value
   * @public
   */
  static $rshiftSet(array, index, value) {
    // TODO: apply array.leafType.narrow() after computation.
    Arrays.$set(array, index, array[index] >> value);
  }

  /**
   * @param {Array<number>} array
   * @param {number} index
   * @param {number} value
   * @public
   */
  static $rshiftUSet(array, index, value) {
    // TODO: apply array.leafType.narrow() after computation.
    Arrays.$set(array, index, array[index] >>> value);
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    ArrayIndexOutOfBoundsException =
        goog.module.get('gen.java.lang.ArrayIndexOutOfBoundsException$impl');
    ArrayStoreException =
        goog.module.get('gen.java.lang.ArrayStoreException$impl');
    Class = goog.module.get('gen.java.lang.Class');
    Object = goog.module.get('gen.java.lang.Object');
    Integer = goog.module.get('gen.java.lang.Integer$impl');
    NullPointerException =
        goog.module.get('gen.java.lang.NullPointerException$impl');
    Casts = goog.module.get('vmbootstrap.Casts$impl');
    Exceptions = goog.module.get('vmbootstrap.Exceptions$impl');
  }
};


/**
 * Is off by default to match default GWT behavior. Can be turned back on once
 * the standard library is fixed to no longer violate this constraint.
 *
 * @define {boolean} Whether or not to check bounds on insertion.
 * @private
 */
goog.define('ARRAY_CHECK_BOUNDS_', false);


/**
 * @define {boolean} Whether or not to check type on insertion.
 * @private
 */
goog.define('ARRAY_CHECK_TYPES_', true);


/**
 * Exported class.
 */
exports = Arrays;
