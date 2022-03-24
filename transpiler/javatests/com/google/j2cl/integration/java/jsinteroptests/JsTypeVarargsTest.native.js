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
 * @param {...*} args
 * @return {number}
 */
JsTypeVarargsTest.varargsLengthThruArguments = function(...args) {
  return arguments.length;
};

/**
 * @return {*}
 */
JsTypeVarargsTest.callGetVarargsSlotUsingJsName = function() {
  return JsTypeVarargsTest.getVarargsSlot(2, '1', '2', '3', '4');
};

/**
 * @return {number}
 */
JsTypeVarargsTest.callSumAndMultiply = function() {
  return JsTypeVarargsTest.sumAndMultiply(2, 10, 20);
};

/**
 * @return {number}
 */
JsTypeVarargsTest.callSumAndMultiplyInt = function() {
  return JsTypeVarargsTest.sumAndMultiplyInt(3, 2, 8);
};

/**
 * @param {Function} f
 * @return {*}
 */
JsTypeVarargsTest.callAFunction = function(f) {
  return f(2, null, null, f, null);
};
