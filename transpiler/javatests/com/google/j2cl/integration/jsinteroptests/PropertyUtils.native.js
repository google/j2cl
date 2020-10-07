// Copyright 2018 Google Inc.
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

const googReflect = goog.require('goog.reflect');

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasPublicMethod = function(obj) {
  return obj[googReflect.objectProperty('publicMethod', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasPublicSubclassMethod = function(obj) {
  return obj[googReflect.objectProperty('publicSubclassMethod', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasPublicStaticSubclassMethod = function(obj) {
  return obj[googReflect.objectProperty('publicStaticSubclassMethod', obj)] !=
      null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasPrivateSubclassMethod = function(obj) {
  return obj[googReflect.objectProperty('privateSubclassMethod', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasProtectedSubclassMethod = function(obj) {
  return obj[googReflect.objectProperty('protectedSubclassMethod', obj)] !=
      null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasPackageSubclassMethod = function(obj) {
  return obj[googReflect.objectProperty('packageSubclassMethod', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasPublicSubclassField = function(obj) {
  return obj[googReflect.objectProperty('publicSubclassField', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasPublicStaticSubclassField = function(obj) {
  return obj[googReflect.objectProperty('publicStaticSubclassField', obj)] !=
      null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasPrivateSubclassField = function(obj) {
  return obj[googReflect.objectProperty('privateSubclassField', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasProtectedSubclassField = function(obj) {
  return obj[googReflect.objectProperty('protectedSubclassField', obj)] !=
      null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasPackageSubclassField = function(obj) {
  return obj[googReflect.objectProperty('packageSubclassField', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasPublicFinalField = function(obj) {
  return obj[googReflect.objectProperty('publicFinalField', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasPrivateStaticFinalField = function(obj) {
  return obj[googReflect.objectProperty('privateStaticFinalField', obj)] !=
      null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasProtectedStaticFinalField = function(obj) {
  return obj[googReflect.objectProperty('protectedStaticFinalField', obj)] !=
      null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasDefaultStaticFinalField = function(obj) {
  return obj[googReflect.objectProperty('defaultStaticFinalField', obj)] !=
      null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasProtectedStaticMethod = function(obj) {
  return obj[googReflect.objectProperty('protectedStaticMethod', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasPrivateStaticMethod = function(obj) {
  return obj[googReflect.objectProperty('privateStaticMethod', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasDefaultStaticMethod = function(obj) {
  return obj[googReflect.objectProperty('defaultStaticMethod', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasOwnPropertyMine = function(obj) {
  return obj.hasOwnProperty(googReflect.objectProperty('mine', obj));
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasOwnPropertyToString = function(obj) {
  return obj.hasOwnProperty(googReflect.objectProperty('toString', obj));
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasNotExported_1 = function(obj) {
  return obj[googReflect.objectProperty('notExported_1', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasNotExported_2 = function(obj) {
  return obj[googReflect.objectProperty('notExported_2', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasNOT_EXPORTED_1 = function(obj) {
  return obj[googReflect.objectProperty('NOT_EXPORTED_1', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasNOT_EXPORTED_2 = function(obj) {
  return obj[googReflect.objectProperty('NOT_EXPORTED_2', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasNOT_EXPORTED_3 = function(obj) {
  return obj[googReflect.objectProperty('NOT_EXPORTED_3', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasNOT_EXPORTED_4 = function(obj) {
  return obj[googReflect.objectProperty('NOT_EXPORTED_4', obj)] != null;
};

/**
 * @param {?} obj
 * @return {boolean}
 */
PropertyUtils.hasNOT_EXPORTED_5 = function(obj) {
  return obj[googReflect.objectProperty('NOT_EXPORTED_5', obj)] != null;
};

let __Class = goog.requireType('java.lang.Class');
/**
 * @template T
 * @param {__Class<T>} clazz
 * @return {function(new:T)}
 */
PropertyUtils.toCtor = function(clazz) {
  return /** @type {?} */ (clazz.f_ctor__java_lang_Class_);
};
