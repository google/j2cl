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

goog.module('jsfunction.JsFunctionTestHelper');

const SomeJsType = goog.requireType('jsfunction.Main.SomeJsType');

/**
 * @return {function(number):number}
 * @public
 */
function createNativeFunction() {
  return function(a) {
    return a;
  };
}

/**
 * @return {function(!SomeJsType):!SomeJsType}
 * @public
 */
function createJsFunctionWithJsType() {
  return function(a) {
    return a;
  };
}



/**
 * @param {function(?number, ?number):?number} fn
 * @return {?number}
 * @public
 */
function callOnFunction(fn) {
  return fn(1.1, 1.1);
}

/**
 * @param {?} fn
 * @param {!RegExp} arg
 * @return {!RegExp}
 * @public
 */
function callAsFunctionWithNativeType(fn, arg) {
  return fn(arg);
}

/**
 * @param {?} fn
 * @param {!SomeJsType} arg
 * @return {!SomeJsType}
 * @public
 */
function callAsFunctionWithJsType(fn, arg) {
  return fn(arg);
}

exports = {
  createNativeFunction,
  createJsFunctionWithJsType,
  callOnFunction,
  callAsFunctionWithNativeType,
  callAsFunctionWithJsType,
};
