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
goog.module('vmbootstrap.Objects$impl');

let Arrays = goog.forwardDeclare('vmbootstrap.Arrays$impl');
let Boolean = goog.forwardDeclare('java.lang.Boolean$impl');
let Class = goog.forwardDeclare('java.lang.Class$impl');
let Double = goog.forwardDeclare('java.lang.Double$impl');
let HashCodes = goog.forwardDeclare('javaemul.internal.HashCodes$impl');
let JavaLangObject = goog.forwardDeclare('java.lang.Object$impl');
let JavaScriptFunction = goog.forwardDeclare('vmbootstrap.JavaScriptFunction$impl');
let JavaScriptObject = goog.forwardDeclare('vmbootstrap.JavaScriptObject$impl');
let String = goog.forwardDeclare('java.lang.String$impl');

/**
 * Provides devirtualized Object methods
 */
class Objects {
  /**
   * @param {*} obj
   * @param {*} other
   * @return {boolean}
   * @public
   */
  static m_equals__java_lang_Object__java_lang_Object(obj, other) {
    Objects.$clinit();

    // Objects: use the custom 'equals' if it exists.
    if (obj.equals) {
      return obj.equals(other);
    }

    // Boxed Types: overrides 'equals' but doesn't need special casing as
    // fallback covers them.

    // Array Types: doesn't override 'equals'.

    // Fallback to default j.l.Object#equals behavior (Equality.$same) except we
    // already know 'obj' is not null.
    return Object.is(obj, other);
  }

  /**
   * @param {*} obj
   * @return {number}
   * @public
   */
  static m_hashCode__java_lang_Object(obj) {
    Objects.$clinit();

    // Objects: use the custom 'hashCode' if it exists.
    if (obj.hashCode) {
      return obj.hashCode();
    }

    // Boxed Types: overrides 'hashCode'  but doesn't need special casing as
    // fallback covers them.

    // Array Types: doesn't override 'hashCode' so fall back cover them.

    // The fallback to default j.l.Object#hashCode behavior.
    return HashCodes.getIdentityHashCode(obj);
  }

  /**
   * @param {*} obj
   * @return {string}
   * @public
   */
  static m_toString__java_lang_Object(obj) {
    return obj.toString();
  }

  /**
   * @param {*} obj
   * @return {Class}
   * @public
   */
  static m_getClass__java_lang_Object(obj) {
    Objects.$clinit();

    // We special case 'getClass' for all types as they all corresspond to
    // different classes.
    let type = typeof obj;
    if (type == 'number') {
      return Class.$get(Double);
    } else if (type == 'boolean') {
      return Class.$get(Boolean);
    } else if (type == 'string') {
      return Class.$get(String);
    } else if (Array.isArray(obj)) {
      return Arrays.m_getClass__java_lang_Object(obj);
    } else if (obj instanceof JavaLangObject) {
      // TODO(b/112664631): use of .constructor on JavaLangObject instances
      // should be allowed.
      return Class.$get(/** @type {!Object} */ (obj).constructor);
    } else if (obj) {
      // Do not need to check existence of 'getClass' since j.l.Object#getClass
      // is final and all native types map to a single special class and so do
      // native functions.
      return Class.$get(
          type == 'function' ? JavaScriptFunction : JavaScriptObject);
    }

    throw new TypeError("null.getClass");
  }

  /**
   * Runs inline static field initializers.
   * @public
   */
  static $clinit() {
    Objects.$clinit = function() {};
    Arrays = goog.module.get('vmbootstrap.Arrays$impl');
    Boolean = goog.module.get('java.lang.Boolean$impl');
    Class = goog.module.get('java.lang.Class$impl');
    Double = goog.module.get('java.lang.Double$impl');
    HashCodes = goog.module.get('javaemul.internal.HashCodes$impl');
    JavaLangObject = goog.module.get('java.lang.Object$impl');
    JavaScriptFunction = goog.module.get('vmbootstrap.JavaScriptFunction$impl');
    JavaScriptObject = goog.module.get('vmbootstrap.JavaScriptObject$impl');
    String = goog.module.get('java.lang.String$impl');
  }
}


/**
 * Exported class.
 */
exports = Objects;
