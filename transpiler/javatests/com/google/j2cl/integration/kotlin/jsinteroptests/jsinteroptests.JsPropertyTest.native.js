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
  return new JsPropertyTest.MyNativeJsType(0);
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

let _MyNativeInterfaceWithProtectedNames = goog.require('woo.JsPropertyTest.MyJsTypeInterfaceWithProtectedNames');
/**
 * @return {_MyNativeInterfaceWithProtectedNames}
 * @public
 */
JsPropertyTest.createMyJsInterfaceWithProtectedNames = function() {
  return new _MyNativeInterfaceWithProtectedNames();
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

JsPropertyTest.MyNativeJsType = class {
  /** @param {number=} x */
  constructor(x) {
    this.x = x;
    this.y = 0;
    this.ctorExecuted = true;
  }

  /**
   * @return {number}
   * @public
   * @nocollapse
   */
  static answerToLife() {
    return 42;
  }

  /**
   * @param {number} bias
   * @return {number}
   * @public
   */
  sum(bias) {
    return this.x + bias;
  }
};

/**
 * @public {number}
 */
JsPropertyTest.MyNativeJsType.staticX = 33;

/** @interface */
JsPropertyTest.AccidentalOverridePropertyJsTypeInterface = class {
  /** @return {number} */
  get x() {
    return 0;
  }
};

/** @interface */
JsPropertyTest.MyNativeJsTypeInterface = class {
  /**
   * @param {number} value
   * @returns {void}
   */
  set x(value) {}

  /**
   * @returns {number}
   */
  get x() {}

  /**
   * @param {number} bias
   * @return {number}
   * @public
   */
  sum(bias) {}
};
