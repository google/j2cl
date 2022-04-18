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


let Class = goog.forwardDeclare('java.lang.Class');
const Constructor = goog.require('javaemul.internal.Constructor');
let InternalPreconditions = goog.forwardDeclare('javaemul.internal.InternalPreconditions$impl');
let JavaLangObject = goog.forwardDeclare('java.lang.Object');
let Objects = goog.forwardDeclare('vmbootstrap.Objects$impl');
const Util = goog.require('nativebootstrap.Util$impl');

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
   * @param {Object} leafType
   * @return {Array<*>}
   * @public
   */
  static $create(dimensionLengths, leafType) {
    return Arrays.$createInternal_(
        dimensionLengths, /** @type {Constructor} */ (leafType),
        leafType.$isInstance, leafType.$initialArrayValue);
  }

  /**
   * Creates, initializes, and returns a native array with the given
   * number of dimensions.
   * Note that this is only used for multi dimension native array creation since
   * single dimension array creation uses a faster code path.
   *
   * @param {Array<number>} dimensionLengths
   * @return {Array<*>}
   * @public
   */
  static $createNative(dimensionLengths) {
    return Arrays.$createRecursiveInternal_(dimensionLengths, null, undefined);
  }

  /**
   * @param {Array<number>} dimensionLengths
   * @param {Constructor} leafType
   * @param {Function} leafTypeIsInstance
   * @param {*} leafTypeInitialValue
   * @return {Array<*>}
   * @private
   */
  static $createInternal_(
      dimensionLengths, leafType, leafTypeIsInstance, leafTypeInitialValue) {
    return Arrays.$createRecursiveInternal_(
        dimensionLengths, leafTypeInitialValue,
        Arrays.$createMetadata_(
            leafType, leafTypeIsInstance, dimensionLengths.length));
  }

  /**
   * @param {Array<number>} dimensionLengths
   * @param {*} leafTypeInitialValue
   * @param {Arrays.Metadata_} metadata
   * @return {Array<*>}
   * @private
   */
  static $createRecursiveInternal_(dimensionLengths, leafTypeInitialValue, metadata) {
    let length = dimensionLengths[0];
    if (length == null) {
      return null;
    }
    // TODO(b/229137602): Use Array when it stops confusing JsCompiler.
    const array = new globalThis.Array(length);
    if (metadata) {
      array.$$arrayMetadata = metadata;
    }

    if (dimensionLengths.length > 1) {
      // Contains sub arrays.
      let subDimensionLengths = dimensionLengths.slice(1);
      let subComponentMetadata =
          metadata && Arrays.$createSubComponentMetadata_(metadata);
      for (let i = 0; i < length; i++) {
        array[i] = Arrays.$createRecursiveInternal_(
            subDimensionLengths, leafTypeInitialValue, subComponentMetadata);
      }
    } else {
      // Contains leaf type values.
      // We only initialize if initial value is different than JS default value.
      if (leafTypeInitialValue !== undefined) {
        for (let index = 0; index < length; index++) {
          array[index] = leafTypeInitialValue;
        }
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
   *
   * @param {Array<*>} array
   * @param {Object} leafType
   * @param {number=} opt_dimensionCount
   * @return {Array<*>}
   * @public
   */
  static $init(array, leafType, opt_dimensionCount) {
    return Arrays.$initInternal_(
        array, /** @type {Constructor} */ (leafType), leafType.$isInstance,
        opt_dimensionCount || 1);
  }

  /**
   * @param {Array<*>} array
   * @param {Constructor} leafType
   * @param {Function} leafTypeIsInstance
   * @param {number} dimensionCount
   * @return {Array<*>}
   * @private
   */
  static $initInternal_(array, leafType, leafTypeIsInstance, dimensionCount) {
    return Arrays.$initRecursiveInternal_(
        array,
        Arrays.$createMetadata_(leafType, leafTypeIsInstance, dimensionCount)

    );
  }

  /**
   * @param {Array<*>} array
   * @param {Arrays.Metadata_} metadata
   * @return {Array<*>}
   * @private
   */
  static $initRecursiveInternal_(array, metadata) {
    array.$$arrayMetadata = metadata;

    if (metadata.dimensionCount > 1) {
      let subComponentMetadata = Arrays.$createSubComponentMetadata_(metadata);
      for (let i = 0; i < array.length; i++) {
        let nestedArray = /** @type {Array<*>} */ (array[i]);
        if (nestedArray) {
          Arrays.$initRecursiveInternal_(nestedArray, subComponentMetadata);
        }
      }
    }

    return array;
  }

  /**
   * @param {Array<*>} array
   * @param {Object} leafType
   * @param {number} dimensionCount
   * @return {Array<*>}
   * @public
   */
  static $stampType(array, leafType, dimensionCount) {
    return Arrays.$stampTypeInternal_(
        array,
        Arrays.$createMetadata_(
            /** @type {Constructor} */ (leafType), leafType.$isInstance,
            dimensionCount));
  }

  /**
   * @param {Array<*>} array
   * @param {Arrays.Metadata_} metadata
   * @return {Array<*>}
   * @private
   */
  static $stampTypeInternal_(array, metadata) {
    array.$$arrayMetadata = metadata;
    return array;
  }

  /**
   * @param {!*} array
   * @return {boolean}
   * @public
   */
  static $isStamped(array) {
    return !!array.$$arrayMetadata;
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
    // Only check when the array has metadata.
    var metadata = Arrays.$getMetadata_(array);
    if (metadata) {
      if (metadata.dimensionCount > 1) {
        if (!Arrays.$instanceIsOfTypeInternal_(
                value, metadata.leafType, metadata.leafTypeIsInstance,
                metadata.dimensionCount - 1)) {
          // The inserted array must fit dimensions and the array leaf type.
          return false;
        }
      } else if (value != null && !metadata.leafTypeIsInstance(value)) {
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
    array.$$arrayMetadata = Arrays.$getMetadata_(otherArray);
  }

  /**
   * Returns whether the given instance is an array, whether it has the given
   * number of dimensions and whether its leaf type is assignable from the given
   * leaf type.
   *
   * @param {*} instance
   * @param {Object} requiredLeafType
   * @param {number} requiredDimensionCount
   * @return {boolean}
   * @public
   */
  static $instanceIsOfType(instance, requiredLeafType, requiredDimensionCount) {
    return Arrays.$instanceIsOfTypeInternal_(
        instance, /** @type {Constructor} */ (requiredLeafType),
        requiredLeafType.$isInstance, requiredDimensionCount);
  }

  /**
   * @param {*} instance
   * @param {Constructor} requiredLeafType
   * @param {Function} requiredLeafTypeIsInstance
   * @param {number} requiredDimensionCount
   * @return {boolean}
   * @private
   */
  static $instanceIsOfTypeInternal_(
      instance, requiredLeafType, requiredLeafTypeIsInstance,
      requiredDimensionCount) {
    Arrays.$clinit();
    if (instance == null || !Array.isArray(instance)) {
      // Null or not an Array can't cast.
      return false;
    }

    const metadata = Arrays.$getMetadata_(instance) ||
        /** @type {Arrays.Metadata_} */ ({
                       leafType: JavaLangObject,
                       dimensionCount: 1
                     });

    const effectiveInstanceDimensionCount = metadata.dimensionCount;
    if (effectiveInstanceDimensionCount == requiredDimensionCount) {
      const fromLeafType = metadata.leafType;

      if (fromLeafType === requiredLeafType) {
        return true;
      }

      if (Util.$isPrimitiveType(requiredLeafType) ||
          Util.$isPrimitiveType(fromLeafType)) {
        // Since they are not the same type, they cannot be compatible if either
        // one is a primitive array.
        return false;
      }

      // Array with same dimension is assignable if the leaf type is assignable.
      return requiredLeafTypeIsInstance(fromLeafType.prototype);
    }

    if (effectiveInstanceDimensionCount > requiredDimensionCount) {
      // If shrinking the dimensions then the new leaf type must *be* Object.
      return JavaLangObject == requiredLeafType;
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
   * @param {Object} requiredLeafType
   * @param {number} requiredDimensionCount
   * @return {*}
   * @public
   */
  static $castTo(instance, requiredLeafType, requiredDimensionCount) {
    return Arrays.$castToInternal_(
        instance, /** @type {Constructor} */ (requiredLeafType),
        requiredLeafType.$isInstance, requiredDimensionCount);
  }

  /**
   * @param {*} instance
   * @param {Constructor} requiredLeafType
   * @param {Function} requiredLeafTypeIsInstance
   * @param {number} requiredDimensionCount
   * @return {*}
   * @private
   */
  static $castToInternal_(
      instance, requiredLeafType, requiredLeafTypeIsInstance,
      requiredDimensionCount) {
    Arrays.$clinit();
    if (InternalPreconditions.m_isTypeChecked__()) {
      const castSucceeds = instance == null ||
          Arrays.$instanceIsOfTypeInternal_(
              instance, requiredLeafType, requiredLeafTypeIsInstance,
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
   * @param {Array<*>} obj
   * @return {Class}
   * @public
   */
  static m_getClass__java_lang_Object(obj) {
    Arrays.$clinit();
    var metadata = Arrays.$getMetadata_(obj);
    if (metadata) {
      return Class.$get(metadata.leafType, metadata.dimensionCount);
    }
    // Uninitialized arrays lack a 'leafType' but are implicitly Object[].
    return Class.$get(JavaLangObject, 1);
  }

  /**
   * @param {Arrays.Metadata_} metadata
   * @return {Arrays.Metadata_}
   * @private
   */
  static $createSubComponentMetadata_(metadata) {
    return Arrays.$createMetadata_(
        metadata.leafType,
        metadata.leafTypeIsInstance,
        metadata.dimensionCount - 1
    );
  }

  /**
   * @param {Constructor} leafType
   * @param {Function} leafTypeIsInstance
   * @param {number} dimensionCount
   * @return {Arrays.Metadata_}
   * @private
   */
  static $createMetadata_(leafType, leafTypeIsInstance, dimensionCount) {
    return {
      leafType: leafType,
      leafTypeIsInstance: leafTypeIsInstance,
      dimensionCount: dimensionCount
    };
  }

   /**
    * @param {Array<*>} array
    * @return {Arrays.Metadata_}
    * @private
    */
  static $getMetadata_(array) {
    var enhancedArray = /** @type {Arrays.EnhancedArray_} */ (array);
    return enhancedArray.$$arrayMetadata;
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    Arrays.$clinit = function() {};
    Class = goog.module.get('java.lang.Class');
    JavaLangObject = goog.module.get('java.lang.Object');
    Objects = goog.module.get('vmbootstrap.Objects$impl');
    InternalPreconditions =
        goog.module.get('javaemul.internal.InternalPreconditions$impl');
  }
}


/**
 * A typedef for the extra properties added to new arrays which are created via
 * this class. These properties allow for  emulation of Java array semantics.
 *
 * @typedef {{
 *   leafType: Function,
 *   leafTypeIsInstance: Function,
 *   dimensionCount: number,
 * }}
 * @private
 */
Arrays.Metadata_;

/**
 * Arrays.Metadata_ enhanced Array.
 *
 * @typedef {{
 *   $$arrayMetadata: Arrays.Metadata_,
 *   length: number
 * }}
 * @private
 */
Arrays.EnhancedArray_;


/**
 * Exported class.
 */
exports = Arrays;
