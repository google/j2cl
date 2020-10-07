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
ConcreteJsType.hasPublicMethod = function(obj) {
  return obj.publicMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPublicStaticMethod = function(obj) {
  return obj.publicStaticMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPrivateMethod = function(obj) {
  return obj.privateMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasProtectedMethod = function(obj) {
  return obj.protectedMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPackageMethod = function(obj) {
  return obj.packageMethod != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPublicField = function(obj) {
  return obj.publicField != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPublicStaticField = function(obj) {
  return obj.publicStaticField != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPrivateField = function(obj) {
  return obj.privateField != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasProtectedField = function(obj) {
  return obj.protectedField != undefined;
};

/**
 * @param {*} obj
 * @return {boolean}
 */
ConcreteJsType.hasPackageField = function(obj) {
  return obj.packageField != undefined;
};
