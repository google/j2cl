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

goog.module('jsinteroptests.JsTypeVarargsTestHelper');

/**
 * @param {...*} args
 * @return {number}
 */
exports.varargsLengthThruArguments = function(...args) {
  return arguments.length;
};

/**
 * @return {*}
 */
exports.callGetVarargsSlotUsingJsName = function() {
  // Work around a cycle caused by using goog.require('woo.JsTypeVarargsTest').
  const JsTypeVarargsTest = goog.module.get('woo.JsTypeVarargsTest');
  return JsTypeVarargsTest.getVarargsSlot(2, '1', '2', '3', '4');
};

/**
 * @return {?number}
 */
exports.callSumAndMultiply = function() {
  // Work around a cycle caused by using goog.require('woo.JsTypeVarargsTest').
  const JsTypeVarargsTest = goog.module.get('woo.JsTypeVarargsTest');
  return JsTypeVarargsTest.sumAndMultiply(2, 10, 20);
};

/**
 * @return {?number}
 */
exports.callSumAndMultiplyInt = function() {
  // Work around a cycle caused by using goog.require('woo.JsTypeVarargsTest').
  const JsTypeVarargsTest = goog.module.get('woo.JsTypeVarargsTest');
  return JsTypeVarargsTest.sumAndMultiplyInt(3, 2, 8);
};

/**
 * @param {Function} f
 * @return {*}
 */
exports.callAFunction = function(f) {
  return f(2, null, null, f, null);
};
