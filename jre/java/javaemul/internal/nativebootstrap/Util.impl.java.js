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
goog.module('nativebootstrap.Util$impl');

const Constructor = goog.require('javaemul.internal.Constructor');
const jre = goog.require('jre');


/**
 * Miscellaneous utility functions.
 */
class Util {
  /**
   * Return the value defined by a goog.define or the default value
   * if it is not defined.
   *
   * @param {string} name
   * @param {?string=} defaultValue
   * @return {?string}
   * @public
   * @noinline
   */
  // TODO(b/374872678): Remove the indirection through $getDefine.
  static $getDefine(name, defaultValue = null) {
    return jre.getSystemProperty(name, defaultValue);
  }

  /**
   * @param {Constructor} ctor
   * @param {string} name
   * @public
   */
  static $setClassMetadata(ctor, name) {
    ctor.prototype.$$classMetadata = [Util.$makeClassName(name), Util.TYPE_CLASS];
  }

  /**
   * // TODO(b/79389970): change ctor to Function
   * @param {Object} ctor
   * @param {string} name
   * @public
   */
  static $setClassMetadataForInterface(ctor, name) {
    ctor.prototype.$$classMetadata = [Util.$makeClassName(name), Util.TYPE_INTERFACE];
  }

  /**
   * @param {Constructor} ctor
   * @param {string} name
   * @public
   */
  static $setClassMetadataForEnum(ctor, name) {
    ctor.prototype.$$classMetadata = [Util.$makeClassName(name), Util.TYPE_ENUM];
  }

  /**
   * @param {Constructor} ctor
   * @param {Constructor} boxedCtor
   * @param {string} name
   * @param {string} shortName
   * @public
   */
  static $setClassMetadataForPrimitive(ctor, boxedCtor, name, shortName) {
    ctor.prototype.$$classMetadata = [Util.$makeClassName(name), Util.TYPE_PRIMITIVE, shortName];
    // Primitives also marked separately as $isPrimitiveType works even without
    // class metadata.
    ctor.prototype.$$isPrimitive = true;
    ctor.prototype.$$boxedType = boxedCtor;
    boxedCtor.prototype.$$primitiveType = ctor;
  }

  /**
   * Returns whether the provided ctor represents primitive type.
   * @param {Constructor} ctor
   * @return {boolean}
   * @public
   */
  static $isPrimitiveType(ctor) {
    return !!ctor && ctor.prototype.$$isPrimitive;
  }

  /**
   * Return the corresponding primitive constructor, if it exists.
   * @param {Constructor} ctor
   * @return {?Constructor}
   * @public
   */
  static $getPrimitiveConstructor(ctor) {
    return ctor.prototype.$$primitiveType;
  }

  /**
   * Return the corresponding boxed constructor, if ctor is a primitive.
   * @param {Constructor} ctor
   * @return {?Constructor}
   * @public
   */
  static $getBoxedConstructor(ctor) {
    return ctor.prototype.$$boxedType || ctor;
  }

  /**
   * @param {Constructor} ctor
   * @return {string}
   * @public
   */
  static $extractClassName(ctor) {
    if (jre.classMetadata == 'SIMPLE') {
      return ctor.prototype.$$classMetadata[0];
    } else if (jre.classMetadata == 'STRIPPED') {
      if (goog.DEBUG) {
        return ctor.prototype.$$classMetadata[0] + '_' +
            Util.$getGeneratedClassName_(ctor);
      }
      return Util.$getGeneratedClassName_(ctor);
    } else {
      throw new Error('Incorrect value: ' + jre.classMetadata);
    }
  }

  /**
   * @param {Constructor} ctor
   * @return {string}
   * @public
   */
  static $extractPrimitiveShortName(ctor) {
    if (jre.classMetadata == 'SIMPLE') {
      return ctor.prototype.$$classMetadata[2];
    } else if (jre.classMetadata == 'STRIPPED') {
      if (goog.DEBUG) {
        return ctor.prototype.$$classMetadata[2] + '_' +
            Util.$getGeneratedClassName_(ctor);
      }
      return Util.$getGeneratedClassName_(ctor);
    } else {
      throw new Error('Incorrect value: ' + jre.classMetadata);
    }
  }

  /**
   * @param {Constructor} ctor
   * @return {string}
   * @private
   * @nosideeffects
   */
  static $getGeneratedClassName_(ctor) {
    const propName = '$$generatedClassName';
    return ctor.prototype.hasOwnProperty(propName) ?
        ctor.prototype[propName] :
        ctor.prototype[propName] = 'Class$obf_' + ++Util.$nextUniqId_;
  }

  /**
   * @param {Constructor} ctor
   * @return {number}
   * @public
   */
  static $extractClassType(ctor) {
    if (jre.classMetadata == 'SIMPLE') {
      return ctor.prototype.$$classMetadata[1];
    } else if (jre.classMetadata == 'STRIPPED') {
      return Util.TYPE_CLASS;
    } else {
      throw new Error('Incorrect value: ' + jre.classMetadata);
    }
  }

  /**
   * Create a function that applies the specified jsFunctionMethod on itself,
   * and copies `instance`' properties to itself.
   *
   * @param {T} jsFunctionMethod
   * @param {U} instance
   * @param {function(U,T):void} copyMethod
   * @return {!T}
   * @template T,U
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
   * Asserts if class for the provided instance has initialized.
   *
   * @param {*} instance
   */
  static $assertClinit(instance) {
    if (COMPILED) {
      return;
    }

    const clinit = instance.constructor['$clinit'];
    if (clinit && clinit.name == '$clinit' /* i.e. not re-written yet */
        && !Util.isEnumInstance(instance)) {
      throw new Error(Util.getInitializationError_(instance.constructor));
    }
  }

  /**
   * @param {*} instance
   * @return {boolean}
   * @private
   */
  static isEnumInstance(instance) {
    let superCtor =
        Object.getPrototypeOf(instance.constructor.prototype).constructor;
    return superCtor.name == 'Enum';
  }

  static getInitializationError_(ctor) {
    let javaCtor = ctor, childCtor = ctor;
    while (!javaCtor.hasOwnProperty('$clinit')) {
      childCtor = javaCtor;
      // Get the super constructor.
      javaCtor = Object.getPrototypeOf(javaCtor.prototype).constructor;
    }

    return javaCtor != childCtor ?
        `Java class ${javaCtor.name} is extended by ${childCtor.name} but not initialized.` +
            `This usually happens if you are extending a class without JsConstructor.` :
        `Java class ${javaCtor.name} is instantiated but not initialized.` +
            `This usually happens if you are instantiating a class without JsConstructor ` +
            `or missing $clinit call in native.js file.`;
  }

  /**
   * Helper to accept a reference to something that should be synchronized on.
   * No synchronization is actually necessary since JS is singlethreaded but
   * it's important that the parameter be passed since the accessing of it
   * may have side effects that should be preserved.
   *
   * @param {*} value The value to synchronize on.
   * @public
   */
  static $synchronized(value) {}

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {}

  /**
   * Helper function used for metadata obfuscation, string replacement passes
   * can be targeted at this bottleneck.
   *
   * TODO(b/31782198): Because J2ClPass runs before ReplaceStrings and inlines
   * functions, the ReplaceStrings pass never sees calls to $setClassMetadata,
   * which makes this function neccessary.
   *
   * @param {string} className
   * @return {string}
   */
  static $makeClassName(className) {
    return className;
  }

  /**
   * Helper function used for enum obfuscation, string replacement passes
   * can be targeted at this bottleneck.
   *
   * @param {string} enumName
   * @return {string}
   */
  static $makeEnumName(enumName) {
    return enumName;
  }
}


/**
 * @private {number}
 */
Util.$nextUniqId_ = 1000;

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
