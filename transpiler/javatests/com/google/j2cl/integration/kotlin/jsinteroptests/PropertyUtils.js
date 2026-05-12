// Copyright 2026 Google Inc.
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

goog.module('jsinteroptests.PropertyUtils');

const googReflect = goog.require('goog.reflect');

class PropertyUtils {
  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasPublicMethod(obj) {
    return obj[googReflect.objectProperty('publicMethod', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasPublicSubclassMethod(obj) {
    return obj[googReflect.objectProperty('publicSubclassMethod', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasPublicStaticSubclassMethod(obj) {
    return obj[googReflect.objectProperty('publicStaticSubclassMethod', obj)] !=
        null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasPrivateSubclassMethod(obj) {
    return obj[googReflect.objectProperty('privateSubclassMethod', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasProtectedSubclassMethod(obj) {
    return obj[googReflect.objectProperty('protectedSubclassMethod', obj)] !=
        null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasPackageSubclassMethod(obj) {
    return obj[googReflect.objectProperty('packageSubclassMethod', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasPublicSubclassField(obj) {
    return obj[googReflect.objectProperty('publicSubclassField', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasPublicStaticSubclassField(obj) {
    return obj[googReflect.objectProperty('publicStaticSubclassField', obj)] !=
        null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasPrivateSubclassField(obj) {
    return obj[googReflect.objectProperty('privateSubclassField', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasProtectedSubclassField(obj) {
    return obj[googReflect.objectProperty('protectedSubclassField', obj)] !=
        null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasPackageSubclassField(obj) {
    return obj[googReflect.objectProperty('packageSubclassField', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasPublicFinalField(obj) {
    return obj[googReflect.objectProperty('publicFinalField', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasPrivateStaticFinalField(obj) {
    return obj[googReflect.objectProperty('privateStaticFinalField', obj)] !=
        null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasProtectedStaticFinalField(obj) {
    return obj[googReflect.objectProperty('protectedStaticFinalField', obj)] !=
        null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasDefaultStaticFinalField(obj) {
    return obj[googReflect.objectProperty('defaultStaticFinalField', obj)] !=
        null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasProtectedStaticMethod(obj) {
    return obj[googReflect.objectProperty('protectedStaticMethod', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasPrivateStaticMethod(obj) {
    return obj[googReflect.objectProperty('privateStaticMethod', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasDefaultStaticMethod(obj) {
    return obj[googReflect.objectProperty('defaultStaticMethod', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasOwnPropertyMine(obj) {
    return obj.hasOwnProperty(googReflect.objectProperty('mine', obj));
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasOwnPropertyToString(obj) {
    return obj.hasOwnProperty(googReflect.objectProperty('toString', obj));
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasNotExported_1(obj) {
    return obj[googReflect.objectProperty('notExported_1', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasNotExported_2(obj) {
    return obj[googReflect.objectProperty('notExported_2', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasNOT_EXPORTED_1(obj) {
    return obj[googReflect.objectProperty('NOT_EXPORTED_1', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasNOT_EXPORTED_2(obj) {
    return obj[googReflect.objectProperty('NOT_EXPORTED_2', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasNOT_EXPORTED_3(obj) {
    return obj[googReflect.objectProperty('NOT_EXPORTED_3', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasNOT_EXPORTED_4(obj) {
    return obj[googReflect.objectProperty('NOT_EXPORTED_4', obj)] != null;
  }

  /**
   * @param {?} obj
   * @return {boolean}
   */
  static hasNOT_EXPORTED_5(obj) {
    return obj[googReflect.objectProperty('NOT_EXPORTED_5', obj)] != null;
  }
}

exports = PropertyUtils;
