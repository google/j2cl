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

const Long = goog.require('goog.math.Long');

/**
 * @return {Function}
 */
exports.createFunctionThatReturnsThis = function() {
  return function() {
    return this;
  };
};

/**
 * @param {?} fn
 * @return {*}
 * @public
 */
exports.callAsFunctionNoArgument = function(fn) {
  return (/** @type {Function} */ (fn))();
};

/**
 * @param {?} fn
 * @param {?} arg
 * @return {?}
 * @public
 */
exports.callAsFunction = function(fn, arg) {
  return (/** @type {Function} */ (fn))(arg);
};

/**
 * @param {function(!Long): !Long} fn
 * @return {boolean}
 * @public
 */
exports.testDirectJsFunctionLongFromJs = function(fn) {
  const input = Long.fromString('5000000000');
  const result = fn(input);
  return result instanceof Long && result.toString() === '5000000001';
};

/**
 * @param {?} fn
 * @return {?}
 * @public
 */
exports.passThrough = function(fn) {
  return fn;
};

/**
 * @param {?} fn1
 * @param {?} fn2
 * @return {boolean}
 * @public
 */
exports.isSameInJs = function(fn1, fn2) {
  return fn1 === fn2;
};

/**
 * @return {?Function}
 * @public
 */
exports.getNullFunction = function() {
  return null;
};

/**
 * @return {(!Function|undefined)}
 * @public
 */
exports.getUndefinedFunction = function() {
  return undefined;
};

/**
 * @param {?} fn
 * @param {!RegExp} arg
 * @return {!RegExp}
 * @public
 */
exports.callAsFunctionWithNativeType = function(fn, arg) {
  return (/** @type {!Function} */ (fn))(arg);
};

/**
 * @param {?} fn
 * @param {number} arg
 * @return {number}
 * @public
 */
exports.callWithFunctionApply = function(fn, arg) {
  return fn.apply(null, [arg]);
};

/**
 * @param {?} fn
 * @param {number} arg
 * @return {number}
 * @public
 */
exports.callWithFunctionCall = function(fn, arg) {
  return fn.call(null, arg);
};

/**
 * @return {Function}
 * @public
 */
exports.createMyJsFunction = function() {
  var myFunction = function(a) { return a; };
  return myFunction;
};

/**
 * @return {Function}
 * @public
 */
exports.createReferentialFunction = function() {
  function myFunction() {
    return myFunction;
  }
  return myFunction;
};

/**
 * @return {Function}
 * @public
 */
exports.createFunction = function() {
  var fun = function(a) { return a; };
  return fun;
};

/**
 * @return {!Function}
 * @public
 */
exports.createFunctionSingleImpl = function() {
  const fun = function() { return 3; };
  return fun;
};

/**
 * @return {*}
 * @public
 */
exports.createObject = function() {
  var a = {};
  return a;
};

/**
 * @param {?} object
 * @param {string} fieldName
 * @return {boolean}
 * @public
 */
exports.hasField = function(object, fieldName) {
  return object[fieldName] != undefined;
};
