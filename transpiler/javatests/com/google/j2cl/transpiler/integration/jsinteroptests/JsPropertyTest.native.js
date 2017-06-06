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
JsPropertyTest.createMyNativeJsType = function() {
  return new MyNativeJsType(0);
};

/**
 * @return {*}
 * @public
 */
JsPropertyTest.createJsTypeGetProperty = function() {
  var a = {};
  a['x'] = undefined;
  return a;
};

/**
 * @return {*}
 * @public
 */
JsPropertyTest.createJsTypeIsProperty = function() {
  var a = {};
  a['x'] = false;
  return a;
};

/**
 * @return {*}
 * @public
 */
JsPropertyTest.createMyJsInterfaceWithProtectedNames = function() {
  var a = {};
  a['nullField'] = 'nullField';
  a['import'] = 'import';
  a['var'] = function() { return 'var'; };
  return a;
};

/**
 * @param {*} value
 * @return {boolean}
 * @public
 */
JsPropertyTest.isUndefined = function(value) {
  return value === undefined;
};

/**
 * @param {*} object
 * @param {*} fieldName
 * @return {boolean}
 * @public
 */
JsPropertyTest.hasField = function(object, fieldName) {
  return object[fieldName] != undefined;
};

/**
 * @param {*} object
 * @param {*} name
 * @return {number}
 * @public
 */
JsPropertyTest.getProperty = function(object, name) {
  return object[name];
};

/**
 * @param {*} object
 * @param {*} name
 * @param {*} value
 * @public
 */
JsPropertyTest.setProperty = function(object, name, value) {
  object[name] = value;
};
