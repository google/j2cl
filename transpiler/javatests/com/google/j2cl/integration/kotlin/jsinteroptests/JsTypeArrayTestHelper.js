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

goog.module('jsinteroptests.JsTypeArrayTestHelper');


/**
 * @return {Array<*>}
 */
exports.returnJsTypeFromNative = function() {
  return [{}, {}];
};

/**
 * @return {Array<*>}
 */
exports.returnJsTypeWithIdsFromNative = function() {
  return [{id: 1}, {id: 2}];
};

/**
 * @param {?} holder
 */
exports.fillArrayField = function(holder) {
  holder.arrayField = [{}, {}];
};

/**
 * @param {?} holder
 */
exports.fillArrayParam = function(holder) {
  holder.setArrayParam([{}, {}]);
};

/**
 * @return {*}
 */
exports.returnJsType3DimFromNative = function() {
  return [[[{id: 1}, {id: 2}, {}], []]];
};

/**
 * @param {number} i
 * @return {*}
 */
exports.getSimpleJsType = function(i) {
  return {id: i};
};

/**
 * @return {*}
 */
exports.returnObjectArrayFromNative = function() {
  return ['1', '2', '3'];
};

/**
 * @return {*}
 */
exports.returnSomeFunction = function() {
  return function(a) { return a + 2; };
};

/**
 * @param {?} object
 * @return {!Array<?string>}
 */
exports.nonNumericKeys = function(object) {
  let array = Object.getOwnPropertyNames(object).filter(key => !isFinite(key));
  array.sort();
  return array;
};