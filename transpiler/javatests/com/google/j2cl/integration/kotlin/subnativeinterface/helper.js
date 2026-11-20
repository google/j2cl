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

goog.module('subnativeinterface.helper');

const MyNativeGenericInterface = goog.requireType('subnativeinterface.MyNativeGenericInterface');
const MyNativeInterface = goog.requireType('subnativeinterface.MyNativeInterface');
const MyNativeInterfaceWithJsMembers = goog.requireType('subnativeinterface.MyNativeInterfaceWithJsMembers');

/** @implements {MyNativeInterface} */
class JsImpl {
  /** @param {string} name */
  constructor(name) {
    /** @private @const {string} */
    this.name_ = name;
  }

  /**
   * @override
   * @return {string}
   */
  getName() {
    return this.name_;
  }
}

/**
 * @param {string} name
 * @return {!MyNativeInterface}
 */
function createJsImpl(name) {
  return new JsImpl(name);
}

/**
 * @param {!MyNativeInterface} o
 * @return {string}
 */
function callGetName(o) {
  return o.getName();
}

/**
 * @param {!MyNativeInterfaceWithJsMembers} o
 * @return {string}
 */
function callRenamed(o) {
  return o.renamed();
}

/**
 * @param {!MyNativeInterfaceWithJsMembers} o
 * @return {string}
 */
function readProperty(o) {
  return o.property;
}

/**
 * @param {!MyNativeInterfaceWithJsMembers} o
 * @param {string} value
 * @return {void}
 */
function writeProperty(o, value) {
  o.property = value;
}

/**
 * @param {!MyNativeGenericInterface<?>} o
 * @param {*} value
 * @return {*}
 */
function callEcho(o, value) {
  return o.echo(value);
}

/**
 * @param {*} o
 * @return {*}
 */
function passThrough(o) {
  return o;
}

/**
 * @return {*}
 */
function nativeObjectImplementingM() {
  return {m: function() { return 3; }};
}

/**
 * @param {?} object
 * @param {string} name
 * @return {number}
 */
function getProperty(object, name) {
  return object[name];
}

/**
 * @param {?} object
 * @param {string} name
 * @param {?} value
 */
function setProperty(object, name, value) {
  object[name] = value;
}

exports = {
  callEcho,
  callGetName,
  callRenamed,
  createJsImpl,
  getProperty,
  nativeObjectImplementingM,
  passThrough,
  readProperty,
  setProperty,
  writeProperty,
};
