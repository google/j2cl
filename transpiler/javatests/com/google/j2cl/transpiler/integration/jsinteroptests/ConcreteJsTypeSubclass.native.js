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
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPublicMethod = function(obj) {
  return obj.publicMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPublicSubclassMethod = function(obj) {
  return obj.publicSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPublicStaticSubclassMethod = function(obj) {
  return obj.publicStaticSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPrivateSubclassMethod = function(obj) {
  return obj.privateSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasProtectedSubclassMethod = function(obj) {
  return obj.protectedSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPackageSubclassMethod = function(obj) {
  return obj.packageSubclassMethod != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPublicSubclassField = function(obj) {
  return obj.publicSubclassField != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPublicStaticSubclassField = function(obj) {
  return obj.publicStaticSubclassField != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPrivateSubclassField = function(obj) {
  return obj.privateSubclassField != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasProtectedSubclassField = function(obj) {
  return obj.protectedSubclassField != null;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsTypeSubclass.hasPackageSubclassField = function(obj) {
  return obj.packageSubclassField != null;
};
