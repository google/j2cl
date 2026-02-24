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
goog.module('nativehelper');

const BaseJsType = goog.require('wasmcustomdescriptorsjsinterop.BaseJsType');
const SomeJsType = goog.require('wasmcustomdescriptorsjsinterop.SomeJsType');

/**
 * @return {!BaseJsType}
 * @public
 */
function newBaseJsType() {
  return new BaseJsType();
}

/**
 * @param {number} value
 * @return {!SomeJsType}
 * @public
 */
function newSomeJsType(value) {
  return new SomeJsType(value);
}

/**
 * @param {!SomeJsType} someJsType
 * @return {number}
 * @public
 */
function callGetNumber(someJsType) {
  return someJsType.getNumber();
}

/**
 * @param {!SomeJsType} someJsType
 * @return {string}
 * @public
 */
function callGetString(someJsType) {
  return someJsType.getString();
}


exports = {
  newBaseJsType,
  newSomeJsType,
  callGetNumber,
  callGetString,
};
