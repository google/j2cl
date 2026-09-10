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

goog.module('jsproperties.JsPropertyTestHelper');

const MyConcreteJsType = goog.require('jsproperties.MyConcreteJsType');
const MyNativeJsType = goog.require('jsproperties.MyNativeJsType');
const NonJsType = goog.require('jsproperties.Main.NonJsType');

/**
 * @return {number}
 * @public
 */
exports.getMyConcreteJsTypeStaticY = function() {
  return MyConcreteJsType.staticY;
};

/**
 * @param {number} value
 * @public
 */
exports.setMyConcreteJsTypeStaticY = function(value) {
  MyConcreteJsType.staticY = value;
};

/**
 * @return {number}
 * @public
 */
exports.getMyConcreteJsTypeStaticX = function() {
  return MyConcreteJsType.staticX;
};

/**
 * @param {number} value
 * @public
 */
exports.setMyConcreteJsTypeStaticX = function(value) {
  MyConcreteJsType.staticX = value;
};

/**
 * @return {number}
 * @public
 */
exports.getMyConcreteJsTypeStaticAbc = function() {
  return MyConcreteJsType.abc;
};

/**
 * @param {number} value
 * @public
 */
exports.setMyConcreteJsTypeStaticAbc = function(value) {
  MyConcreteJsType.abc = value;
};

/**
 * @return {*}
 * @public
 */
exports.createMyNativeJsType = function() {
  return new MyNativeJsType(0);
};

/**
 * @return {*}
 * @public
 */
exports.createJsTypeGetProperty = function() {
  var a = {};
  a['x'] = undefined;
  return a;
};

/**
 * @return {*}
 * @public
 */
exports.createJsTypeIsProperty = function() {
  var a = {};
  a['x'] = false;
  return a;
};

/**
 * @param {?} value
 * @return {boolean}
 * @public
 */
exports.isUndefined = function(value) {
  return value === undefined;
};

/**
 * @param {?} object
 * @param {string} name
 * @return {number}
 * @public
 */
exports.getProperty = function(object, name) {
  return object[name];
};

/**
 * @param {?} object
 * @param {string} name
 * @param {?} value
 * @public
 */
exports.setProperty = function(object, name, value) {
  object[name] = value;
};

/**
 * @return {number}
 * @public
 */
exports.getNonJsTypeStaticX = function() {
  return NonJsType.staticX;
};

/**
 * @param {number} value
 * @public
 */
exports.setNonJsTypeStaticX = function(value) {
  NonJsType.staticX = value;
};

/**
 * @param {!MyConcreteJsType} object
 * @return {number}
 * @public
 */
exports.getMyConcreteJsTypeAbc = function(object) {
  return object.abc;
};

/**
 * @param {!MyConcreteJsType} object
 * @param {number} value
 * @public
 */
exports.setMyConcreteJsTypeAbc = function(object, value) {
  object.abc = value;
};

/**
 * @param {!NonJsType} object
 * @return {number}
 * @public
 */
exports.getNonJsTypeAbc = function(object) {
  return object.abc;
};

/**
 * @param {!NonJsType} object
 * @param {number} value
 * @public
 */
exports.setNonJsTypeAbc = function(object, value) {
  object.abc = value;
};

/**
 * @return {number}
 * @public
 */
exports.getNonJsTypeStaticAbc = function() {
  return NonJsType.abc;
};

/**
 * @param {number} value
 * @public
 */
exports.setNonJsTypeStaticAbc = function(value) {
  NonJsType.abc = value;
};

