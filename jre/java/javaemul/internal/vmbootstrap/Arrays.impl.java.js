// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

/**
 * Impl hand rolled.
 */
goog.module('vmbootstrap.Arrays$impl');


// Don't reformat these imports! The uncompiled test harness contains a bug
// that will miss some multiline goog.require's.
let Hashing = goog.require('nativebootstrap.Hashing$impl');
let Class = goog.forwardDeclare('java.lang.Class');
let Integer = goog.forwardDeclare('java.lang.Integer$impl');
let InternalPreconditions = goog.forwardDeclare('javaemul.internal.InternalPreconditions$impl');
let Object = goog.forwardDeclare('java.lang.Object');
let Objects = goog.forwardDeclare('vmbootstrap.Objects$impl');

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
        leafType.$isAssignableFrom, leafType.$initialArrayValue);
  }

  /**
   * @param {Array<number>} dimensionLengths
   * @param {*} leafType
   * @param {Function} leafTypeIsInstance
   * @param {Function} leafTypeIsAssignableFrom
   * @param {*} leafTypeInitialValue
   * @return {Array<*>}
   * @private
   */
  static $createInternal(
      dimensionLengths, leafType, leafTypeIsInstance, leafTypeIsAssignableFrom,
      leafTypeInitialValue) {
    let length = dimensionLengths[0];
    if (length == null) {
      return null;
    }

    let array = [];
    array.leafType = leafType;
    array.leafTypeIsInstance = leafTypeIsInstance;
    array.leafTypeIsAssignableFrom = leafTypeIsAssignableFrom;
    array.dimensionCount = dimensionLengths.length;
    array.length = length;

    if (array.dimensionCount > 1) {
      // Contains sub arrays.
      let subDimensionLengths = dimensionLengths.slice(1);
      for (let i = 0; i < length; i++) {
        array[i] = Arrays.$createInternal(
            subDimensionLengths, leafType, leafTypeIsInstance,
            leafTypeIsAssignableFrom, leafTypeInitialValue);
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
        array, leafType, leafType.$isInstance, leafType.$isAssignableFrom,
        opt_dimensionCount || 1);
  }

  /**
   * @param {Array<*>} array
   * @param {*} leafType
   * @param {Function} leafTypeIsInstance
   * @param {Function} leafTypeIsAssignableFrom
   * @param {number} dimensionCount
   * @return {Array<*>}
   * @private
   */
  static $initRecursiveInternal(
      array, leafType, leafTypeIsInstance, leafTypeIsAssignableFrom,
      dimensionCount) {
    array.leafType = leafType;
    array.leafTypeIsInstance = leafTypeIsInstance;
    array.leafTypeIsAssignableFrom = leafTypeIsAssignableFrom;
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
            /** @type {Array<*>} */ (nestedArray), leafType, leafTypeIsInstance,
            leafTypeIsAssignableFrom, dimensionCount - 1);
      }
    }

    return array;
  }

  /**
   * @param {Array<*>} array
   * @param {*} leafType
   * @param {number} dimensionCount
   * @return {Array<*>}
   * @public
   */
  static $stampType(array, leafType, dimensionCount) {
    return Arrays.$stampTypeInternal_(
        array, leafType, leafType.$isInstance, leafType.$isAssignableFrom,
        dimensionCount);
  }

  /**
   * @param {Array<*>} array
   * @param {*} leafType
   * @param {Function} leafTypeIsInstance
   * @param {Function} leafTypeIsAssignableFrom
   * @param {number} dimensionCount
   * @return {Array<*>}
   * @private
   */
  static $stampTypeInternal_(
      array, leafType, leafTypeIsInstance, leafTypeIsAssignableFrom,
      dimensionCount) {
    array.leafType = leafType;
    array.leafTypeIsInstance = leafTypeIsInstance;
    array.leafTypeIsAssignableFrom = leafTypeIsAssignableFrom;
    array.dimensionCount = dimensionCount;
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

    // TODO(goktug) remove m_isTypeChecked when $canSet_ could be marked or
    // proved as side effect free.
    if (InternalPreconditions.m_isTypeChecked__()) {
      InternalPreconditions.m_checkArrayType__boolean(
          value == null || Arrays.$canSet_(array, index, value));
    }

    return array[index] = value;
  }

  /**
   * @template T
   * @param {Array<*>} array
   * @param {number} index
   * @param {T} value
   * @return {boolean}
   * @private
   */
  static $canSet_(array, index, value) {
    // Only check when the array has a known leaf type. JS native arrays won't
    // have it.
    if (/** @type {*} */ (array).leafTypeIsInstance) {
      var enhancedArray = /** @type {Arrays.EnhancedArray_} */ (array);
      if (enhancedArray.dimensionCount > 1) {
        if (!Arrays.$instanceIsOfTypeInternal(
                value, enhancedArray.leafType,
                enhancedArray.leafTypeIsAssignableFrom,
                enhancedArray.dimensionCount - 1)) {
          // The inserted array must fit dimensions and the array leaf type.
          return false;
        }
      } else if (value != null && !enhancedArray.leafTypeIsInstance(value)) {
        // The inserted value must fit the array leaf type.
        // If leafType is not a primitive type, a 'null' should always be a
        // legal value. If leafType is a primitive type, value cannot be null
        // because that is illegal in Java.
        return false;
      }
    }
    return true;
  }

  /**
   * Changes the given array's to the given leafType.
   *
   * @param {Array<*>} array
   * @param {Array<*>} otherArray
   * @public
   */
  static $copyType(array, otherArray) {
    var enhancedArray = /** @type {Arrays.EnhancedArray_} */ (array);
    var otherEnhancedArray = /** @type {Arrays.EnhancedArray_} */ (otherArray);
    enhancedArray.leafType = otherEnhancedArray.leafType;
    enhancedArray.leafTypeIsInstance = otherEnhancedArray.leafTypeIsInstance;
    enhancedArray.leafTypeIsAssignableFrom =
        otherEnhancedArray.leafTypeIsAssignableFrom;
    enhancedArray.dimensionCount = otherEnhancedArray.dimensionCount;
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

    // TODO(michaelthomas): Consider checking that this is an enhanced array
    // before casting.
    var enhancedArray = /** @type {Arrays.EnhancedArray_} */ (instance);

    // One dimensional Object arrays are emitted as a raw JS [] array literal
    // and will be missing the dimensionCount field.
    var effectiveInstanceDimensionCount = (enhancedArray.dimensionCount || 1);

    if (effectiveInstanceDimensionCount == requiredDimensionCount) {
      // If dimensions are equal then the leaftypes must be castable.
      return requiredLeafTypeIsAssignableFrom(enhancedArray.leafType);
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
  static $instanceIsOfNative(instance) {
    return Array.isArray(instance);
  }

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
   * @param {Function} requiredLeafTypeIsAssignableFrom
   * @param {number} requiredDimensionCount
   * @return {*}
   * @public
   */
  static $castToInternal(
      instance, requiredLeafType, requiredLeafTypeIsAssignableFrom,
      requiredDimensionCount) {
    Arrays.$clinit();
    if (InternalPreconditions.m_isTypeChecked__()) {
      const castSucceeds = instance == null ||
          Arrays.$instanceIsOfTypeInternal(
              instance, requiredLeafType, requiredLeafTypeIsAssignableFrom,
              requiredDimensionCount);
      if (!castSucceeds) {
        // We don't delegate to a common throw function because it confuses
        // JSCompiler's inliner and costs 1% code size.
        const castTypeClass =
            Class.$get(requiredLeafType, requiredDimensionCount);
        const instanceTypeClass =
            Objects.m_getClass__java_lang_Object(instance);
        const message = instanceTypeClass.m_getName__() +
            ' cannot be cast to ' + castTypeClass.m_getName__();
        InternalPreconditions.m_checkType__boolean__java_lang_String(
            false, message);
      }
    }
    return instance;
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
    InternalPreconditions.m_checkType__boolean(
        instance == null || Array.isArray(instance));
    return instance;
  }

  /**
   * @param {*} obj
   * @return {string}
   * @public
   */
  static m_toString__java_lang_Object(obj) {
    Arrays.$clinit();
    return Arrays.m_getClass__java_lang_Object(obj).m_getName__() + '@' +
        Integer.m_toHexString__int(Hashing.$getHashCode(obj));
  }

  /**
   * @param {*} obj
   * @return {Class}
   * @public
   */
  static m_getClass__java_lang_Object(obj) {
    Arrays.$clinit();
    if (obj.leafType) {
      return Class.$get(obj.leafType, obj.dimensionCount || 1);
    }
    // Uninitialized arrays lack a 'leafType' but are implicitly Object[].
    return Class.$get(Object, 1);
  }

  /**
   * Ensure the array is not null before returning it.
   * @param {Array<*>} arrayOrNull
   * @return {!Array<*>}
   */
  static $checkNotNull(arrayOrNull) {
    Arrays.$clinit();
    InternalPreconditions.m_checkNotNull__java_lang_Object(arrayOrNull);
    return /** @type {!Array<*>} */ (arrayOrNull);
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    Arrays.$clinit = function() {};
    Class = goog.module.get('java.lang.Class');
    Object = goog.module.get('java.lang.Object');
    Objects = goog.module.get('vmbootstrap.Objects$impl');
    Integer = goog.module.get('java.lang.Integer$impl');
    InternalPreconditions =
        goog.module.get('javaemul.internal.InternalPreconditions$impl');
  }
}


/**
 * A typedef for the extra properties added to new arrays which are created via
 * this class. These properties allow for better emulation of Java array
 * semantics.
 *
 * @typedef {{
 *   leafType: *,
 *   leafTypeIsInstance: Function,
 *   leafTypeIsAssignableFrom: Function,
 *   dimensionCount: number,
 *   length: number
 * }}
 * @private
 */
Arrays.EnhancedArray_;


/**
 * Exported class.
 */
exports = Arrays;
