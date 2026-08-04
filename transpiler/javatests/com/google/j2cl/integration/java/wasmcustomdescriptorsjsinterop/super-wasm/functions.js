/*
 * Copyright 2026 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
goog.module('functions');

/**
 * @return {function(number): number}
 * @public
 */
function getFunction() {
  return (a) => a + 32;
}

/**
 * @return {?}
 * @public
 */
function getFunctionAsObject() {
  return (a) => a + 32;
}

/**
 * @return {function(?): ?}
 * @public
 */
function getFunctionWithObject() {
  return (a) => a;
}

/**
 * @param {function(number): number} fn
 * @param {number} a
 * @return {number}
 * @public
 */
function callFunction(fn, a) {
  return fn(a);
}

/**
 * @param {?} fn
 * @param {number} a
 * @return {number}
 * @public
 */
function callFunctionAsObject(fn, a) {
  return fn(a);
}

/**
 * @param {function(?): ?} fn
 * @param {?} a
 * @return {?}
 * @public
 */
function callFunctionWithObject(fn, a) {
  return fn(a);
}

/**
 * @param {function(number): number} fn
 * @return {function(number): number}
 * @public
 */
function passThrough(fn) {
  return fn;
}

/**
 * @param {?} fn
 * @return {?}
 * @public
 */
function passThroughAsObject(fn) {
  return fn;
}

/**
 * @param {function(number): number} fn1
 * @param {function(number): number} fn2
 * @return {boolean}
 * @public
 */
function isSame(fn1, fn2) {
  return fn1 === fn2;
}

exports = {
  getFunction,
  getFunctionAsObject,
  getFunctionWithObject,
  callFunction,
  callFunctionAsObject,
  callFunctionWithObject,
  passThrough,
  passThroughAsObject,
  isSame
};
