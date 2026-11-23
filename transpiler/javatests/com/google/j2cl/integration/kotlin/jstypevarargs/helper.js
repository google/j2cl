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

goog.module('jstypevarargs.helper');

const Varargs = goog.require('jstypevarargs.Varargs');

/**
 * @return {?number}
 */
exports.callSumAndMultiply = function() {
  return Varargs.sumAndMultiply(10, 1, 2);
};

/**
 * @return {number}
 */
exports.callF1 = function() {
  return Varargs.f1(10, 1, 2);
};

/**
 * @return {number}
 */
exports.callF2 = function() {
  return Varargs.f2(1, 2);
};

/**
 * @param {!Varargs} m
 * @return {number}
 */
exports.callF3 = function(m) {
  return m.f3(10, 1, 2);
};

/**
 * @param {!Varargs} m
 * @return {number}
 */
exports.callF4 = function(m) {
  return m.f4(1, 2);
};

/**
 * @param {*} a
 * @return {number}
 */
exports.callJsFunction = function(a) {
  return (/** @type {!Function} */ (a))(0, null, null);
};

/**
 * @param {...*} values
 * @return {number}
 */
exports.countArgs = function(...values) {
  return values.length;
};
