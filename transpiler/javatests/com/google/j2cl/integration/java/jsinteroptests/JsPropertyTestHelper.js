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

goog.module('jsinteroptests.JsPropertyTestHelper');

const MyNativeJsType = goog.require('jsinteroptests.JsPropertyTest.MyNativeJsType');

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

let _MyNativeInterfaceWithProtectedNames = goog.require('woo.JsPropertyTest.MyJsTypeInterfaceWithProtectedNames');
/**
 * @return {_MyNativeInterfaceWithProtectedNames}
 * @public
 */
exports.createMyJsInterfaceWithProtectedNames = function() {
  return new _MyNativeInterfaceWithProtectedNames();
};

/**
 * @param {*} value
 * @return {boolean}
 * @public
 */
exports.isUndefined = function(value) {
  return value === undefined;
};

/**
 * @param {*} object
 * @param {*} fieldName
 * @return {boolean}
 * @public
 */
exports.hasField = function(object, fieldName) {
  return object[fieldName] != undefined;
};

/**
 * @param {*} object
 * @param {*} name
 * @return {number}
 * @public
 */
exports.getProperty = function(object, name) {
  return object[name];
};

/**
 * @param {*} object
 * @param {*} name
 * @param {*} value
 * @public
 */
exports.setProperty = function(object, name, value) {
  object[name] = value;
};
