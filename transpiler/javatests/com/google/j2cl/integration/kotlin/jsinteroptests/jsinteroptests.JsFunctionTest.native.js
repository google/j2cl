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
 * @return {Function}
 */
JsFunctionTest.createFunctionThatReturnsThis = function() {
  return function() {
    return this;
  };
};

/**
 * @param {*} fn
 * @return {*}
 * @public
 */
JsFunctionTest.callAsFunctionNoArgument = function(fn) {
  return (/** @type {Function} */ (fn))();
};

/**
 * @param {*} fn
 * @param {number} arg
 * @return {number}
 * @public
 */
JsFunctionTest.callAsFunction = function(fn, arg) {
  return (/** @type {Function} */ (fn))(arg);
};

/**
 * @param {*} fn
 * @param {number} arg
 * @return {number}
 * @public
 */
JsFunctionTest.callWithFunctionApply = function(fn, arg) {
  return fn.apply(null, [arg]);
};

/**
 * @param {*} fn
 * @param {number} arg
 * @return {number}
 * @public
 */
JsFunctionTest.callWithFunctionCall = function(fn, arg) {
  return fn.call(null, arg);
};

/**
 * @param {*} object
 * @param {string} fieldName
 * @param {number} value
 * @public
 */
JsFunctionTest.setField = function(object, fieldName, value) {
  object[fieldName] = value;
};

/**
 * @param {*} object
 * @param {string} fieldName
 * @return {number}
 * @public
 */
JsFunctionTest.getField = function(object, fieldName) {
  return object[fieldName];
};

/**
 * @param {*} object
 * @param {string} functionName
 * @return {number}
 * @public
 */
JsFunctionTest.callIntFunction = function(object, functionName) {
  return object[functionName]();
};

/**
 * @return {Function}
 * @public
 */
JsFunctionTest.createMyJsFunction = function() {
  var myFunction = function(a) { return a; };
  return myFunction;
};

/**
 * @return {Function}
 * @public
 */
JsFunctionTest.createReferentialFunction = function() {
  function myFunction() {
    return myFunction;
  }
  return myFunction;
};

/**
 * @return {Function}
 * @public
 */
JsFunctionTest.createFunction = function() {
  var fun = function(a) { return a; };
  return fun;
};

/**
 * @return {*}
 * @public
 */
JsFunctionTest.createObject = function() {
  var a = {};
  return a;
};

/**
 * @param {*} object
 * @param {?string} fieldName
 * @return {boolean}
 * @public
 */
JsFunctionTest.hasField = function(object, fieldName) {
  return object[fieldName] != undefined;
};
