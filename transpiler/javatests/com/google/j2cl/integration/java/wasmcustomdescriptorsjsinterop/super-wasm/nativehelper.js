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
const JsInterface = goog.require('wasmcustomdescriptorsjsinterop.JsInterface');
const JsInterfaceGetNumber = goog.require('wasmcustomdescriptorsjsinterop.JsInterfaceGetNumber');
const JsInterfaceRenamedMethod = goog.require('wasmcustomdescriptorsjsinterop.JsInterfaceRenamedMethod');
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

/**
 * @param {!SomeJsType} someJsType
 * @return {number}
 * @public
 */
function getField(someJsType) {
  return someJsType.field;
}

/**
 * @param {!SomeJsType} someJsType
 * @param {number} value
 * @return {void}
 * @public
 */
function setField(someJsType, value) {
  someJsType.field = value;
}

/**
 * @return {number}
 * @public
 */
function getStaticField() {
  return SomeJsType.staticField;
}

/**
 * @param {number} value
 * @return {void}
 * @public
 */
function setStaticField(value) {
  SomeJsType.staticField = value;
}

/**
 * @param {!SomeJsType} someJsType
 * @return {number}
 * @public
 */
function getReadOnlyField(someJsType) {
  return someJsType.readOnlyField;
}

/**
 * @return {number}
 * @public
 */
function getStaticReadOnlyField() {
  return SomeJsType.staticReadOnlyField;
}

/**
 * @param {!SomeJsType} someJsType
 * @return {number}
 * @public
 */
function getReadOnlyProperty(someJsType) {
  return someJsType.readOnlyProperty;
}

/**
 * @return {number}
 * @public
 */
function getStaticReadOnlyProperty() {
  return SomeJsType.staticReadOnlyProperty;
}

/**
 * @param {!SomeJsType} someJsType
 * @return {number}
 * @public
 */
function getReadWriteProperty(someJsType) {
  return someJsType.readWriteProperty;
}

/**
 * @param {!SomeJsType} someJsType
 * @param {number} value
 * @return {void}
 * @public
 */
function setReadWriteProperty(someJsType, value) {
  someJsType.readWriteProperty = value;
}

/**
 * @param {!SomeJsType} someJsType
 * @return {string}
 * @public
 */
function callPackagePrivateMethod(someJsType) {
  return someJsType.packagePrivateMethod();
}

/**
 * @param {!JsInterface} i
 * @return {number}
 * @public
 */
function callInterfaceMethod(i) {
  return i.interfaceMethod();
}

/**
 * @param {!JsInterfaceGetNumber} i
 * @return {number}
 * @public
 */
function callInterfaceGetNumber(i) {
  return i.getNumber();
}

/**
 * @param {!JsInterfaceRenamedMethod} i
 * @return {number}
 * @public
 */
function callInterfaceRenamedMethod(i) {
  return i.renamed();
}

/**
 * @param {!JsInterfaceDefaultMethod} i
 * @return {number}
 * @public
 */
function callInterfaceDefaultMethod(i) {
  return i.m();
}

exports = {
  newBaseJsType,
  newSomeJsType,
  callGetNumber,
  callGetString,
  getField,
  setField,
  getStaticField,
  setStaticField,
  getReadOnlyField,
  getStaticReadOnlyField,
  getReadOnlyProperty,
  getStaticReadOnlyProperty,
  getReadWriteProperty,
  setReadWriteProperty,
  callPackagePrivateMethod,
  callInterfaceMethod,
  callInterfaceGetNumber,
  callInterfaceRenamedMethod,
  callInterfaceDefaultMethod,
};
