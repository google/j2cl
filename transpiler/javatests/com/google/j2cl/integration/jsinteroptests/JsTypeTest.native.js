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
 * @return {*}
 * @public
 */
JsTypeTest.createNativeButton = function() {
  return document.createElement('button');
};

/**
 * @return {*}
 * @public
 */
JsTypeTest.createObject = function() {
  return {};
};

/**
 * @param {*} object
 * @return {number}
 */
JsTypeTest.callPublicMethod = function(object) {
  return object.publicMethod();
};

/**
 * @param {*} value
 * @return {boolean}
 */
JsTypeTest.isUndefined = function(value) {
  return value == undefined;
};

/**
 * @param {*} obj
 * @param {*} value
 */
JsTypeTest.setTheField = function(obj, value) {
  obj.notTypeTightenedField = value;
};

/**
 * @param {*} obj
 * @param {*} param
 * @return {*}
 */
JsTypeTest.callFoo = function(obj, param) {
  return obj.foo(param);
};

/**
 * @param {*} obj
 * @param {*} param
 * @return {*}
 */
JsTypeTest.callBar = function(obj, param) {
  return obj.bar(param);
};

/**
 * @param {*} obj
 * @param {*} param
 * @return {*}
 */
JsTypeTest.callM = function(obj, param) {
  return obj.m(param);
};

/**
 * @param {*} jstype
 */
JsTypeTest.fillJsTypeField = function(jstype) {
  jstype.someField = {};
};

/**
 * @return {*}
 */
JsTypeTest.nativeObjectImplementingM = function() {
  return {m: function() { return 3; }};
};

/**
 * @return {*}
 */
JsTypeTest.nativeJsFunction = function() {
  return function() { return 3; };
};

/**
 * @param {*} obj
 * @return {boolean}
 */
JsTypeTest.hasFieldRun = function(obj) {
  return obj.run != undefined;
};

/**
 * @param {*} enumeration
 * @return {number}
 */
JsTypeTest.callPublicMethodFromEnumeration = function(enumeration) {
  return enumeration.idxAddOne();
};

/**
 * @param {*} enumeration
 * @return {number}
 */
JsTypeTest.callPublicMethodFromEnumerationSubclass = function(enumeration) {
  return enumeration.foo();
};


/** @interface */
JsTypeTest.InterfaceWithSingleJavaConcrete = class {
  /** @return {number} */
  m() {}
};
