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

goog.module('jsinteroptests.JsTypeTestHelper');

/**
 * @fileoverview
 * @suppress {strictMissingProperties}
 */


/**
 * @return {*}
 * @public
 */
exports.createNativeButton = function() {
  return document.createElement('button');
};

/**
 * @return {*}
 * @public
 */
exports.createObject = function() {
  return {};
};

/**
 * @param {*} object
 * @return {number}
 */
exports.callPublicMethod = function(object) {
  return object.publicMethod();
};

/**
 * @param {*} value
 * @return {boolean}
 */
exports.isUndefined = function(value) {
  return value == undefined;
};

/**
 * @param {*} obj
 * @param {*} value
 */
exports.setTheField = function(obj, value) {
  obj.notTypeTightenedField = value;
};

/**
 * @param {*} obj
 * @param {*} param
 * @return {*}
 */
exports.callFoo = function(obj, param) {
  return obj.foo(param);
};

/**
 * @param {*} obj
 * @param {*} param
 * @return {*}
 */
exports.callBar = function(obj, param) {
  return obj.bar(param);
};

/**
 * @param {*} obj
 * @param {*} param
 * @return {*}
 */
exports.callM = function(obj, param) {
  return obj.m(param);
};

/**
 * @param {*} jstype
 */
exports.fillJsTypeField = function(jstype) {
  jstype.someField = {};
};

/**
 * @return {*}
 */
exports.nativeObjectImplementingM = function() {
  return {m: function() { return 3; }};
};

/**
 * @return {*}
 */
exports.nativeJsFunction = function() {
  return function() { return 3; };
};

/**
 * @param {*} obj
 * @return {boolean}
 */
exports.hasFieldRun = function(obj) {
  return obj.run != undefined;
};

/**
 * @param {*} enumeration
 * @return {number}
 */
exports.callPublicMethodFromEnumeration = function(enumeration) {
  return enumeration.idxAddOne();
};

/**
 * @param {*} enumeration
 * @return {number}
 */
exports.callPublicMethodFromEnumerationSubclass = function(enumeration) {
  return enumeration.foo();
};
