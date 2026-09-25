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

const AbstractJsType = goog.require('wasmcustomdescriptorsjsinterop.AbstractJsType');
const BaseJsType = goog.require('wasmcustomdescriptorsjsinterop.BaseJsType');
const CapturesOuter = goog.require('wasmcustomdescriptorsjsinterop.SomeJsType.CapturesOuter');
const IntValuedJsEnum = goog.require('wasmcustomdescriptorsjsinterop.IntValuedJsEnum');
const JsInterface = goog.require('wasmcustomdescriptorsjsinterop.JsInterface');
const JsInterfaceAccidentalDefaultMethodImpl = goog.require('wasmcustomdescriptorsjsinterop.Main.JsInterfaceAccidentalDefaultMethodImpl');
const JsInterfaceAccidentalImpl = goog.require('wasmcustomdescriptorsjsinterop.Main.JsInterfaceAccidentalImpl');
const JsInterfaceDefaultMethod = goog.require('wasmcustomdescriptorsjsinterop.JsInterfaceDefaultMethod');
const JsInterfaceGetNumber = goog.require('wasmcustomdescriptorsjsinterop.JsInterfaceGetNumber');
const JsInterfaceRenamedMethod = goog.require('wasmcustomdescriptorsjsinterop.JsInterfaceRenamedMethod');
const JsInterfaceStaticMethod = goog.require('wasmcustomdescriptorsjsinterop.JsInterfaceStaticMethod');
const Long = goog.require('goog.math.Long');
const MyNativeType = goog.require('nativehelper.MyNativeType');
const NativeJsType = goog.require('native.NativeJsType');
const NativeJsTypeConsumer = goog.require('wasmcustomdescriptorsjsinterop.NativeJsTypeConsumer');
const SimpleJsEnum = goog.require('wasmcustomdescriptorsjsinterop.SimpleJsEnum');
const SomeJsType = goog.require('wasmcustomdescriptorsjsinterop.SomeJsType');
const StringValuedJsEnum = goog.require('wasmcustomdescriptorsjsinterop.StringValuedJsEnum');

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
 * @return {number}
 * @public
 */
function callGetNumberViaStaticMethod(someJsType) {
  return SomeJsType.staticMethod(someJsType);
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
 * @return {!Long}
 * @public
 */
function callGetLong(someJsType) {
  return someJsType.getLong();
}

/**
 * @param {!SomeJsType} someJsType
 * @return {!Long}
 * @public
 */
function callGetPrimitiveLong(someJsType) {
  return someJsType.getPrimitiveLong();
}

/**
 * @param {!SomeJsType} someJsType
 * @param {!Long} a
 * @param {!Long} b
 * @return {!Long}
 * @public
 */
function callAddPrimitiveLong(someJsType, a, b) {
  return someJsType.addPrimitiveLong(a, b);
}

/**
 * @param {!SomeJsType} someJsType
 * @return {!NativeJsType}
 * @public
 */
function callGetNativeJsType(someJsType) {
  return someJsType.getNativeJsType();
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
 * @param {!SomeJsType} someJsType
 * @return {!Long}
 * @public
 */
function getLongField(someJsType) {
  return someJsType.longField;
}

/**
 * @param {!SomeJsType} someJsType
 * @param {!Long} value
 * @return {void}
 * @public
 */
function setLongField(someJsType, value) {
  someJsType.longField = value;
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
 * @return {number}
 * @public
 */
function getStaticFieldSameJsNameAsInstanceField() {
  return SomeJsType.readOnlyField;
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
 * @return {number}
 * @public
 */
function getStaticPropertySameJsNameAsInstanceProperty() {
  return SomeJsType.readOnlyProperty;
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
 * @return {?string}
 * @public
 */
function callPackagePrivateMethod(someJsType) {
  return someJsType.packagePrivateMethod();
}

/**
 * @param {!SomeJsType} someJsType
 * @return {?SomeJsType}
 * @public
 */
function callReturnSelf(someJsType) {
  return someJsType.returnSelf();
}

/**
 * @param {!SomeJsType} someJsType
 * @param {!SomeJsType} arg
 * @return {boolean}
 * @public
 */
function callTakesSelf(someJsType, arg) {
  return someJsType.takesSelf(arg);
}

/**
 * @param {!SomeJsType} someJsType
 * @param {*} o
 * @param {string} s
 * @param {number} i
 * @return {number}
 * @public
 */
function callMethodWithTypeParameters(someJsType, o, s, i) {
  return someJsType.withTypeParameters(o, s, i);
}

/**
 * @param {!SomeJsType} someJsType
 * @return {!CapturesOuter}
 * @public
 */
function newCapturesOuter(someJsType) {
  return new CapturesOuter(someJsType);
}

/**
 * @param {!CapturesOuter} capturesOuter
 * @return {?SomeJsType}
 * @public
 */
function callGetOuter(capturesOuter) {
  return capturesOuter.getOuter();
}

/**
 * @param {!AbstractJsType} someJsType
 * @return {number}
 * @public
 */
function callAbstractMethod(someJsType) {
  return someJsType.abstractMethod();
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

/**
 * @return {number}
 * @public
 */
function callInterfaceStaticMethod() {
  return JsInterfaceStaticMethod.staticMethod();
}

/**
 * @return {number}
 * @public
 */
function callInterfaceStaticProperty() {
  return JsInterfaceStaticMethod.staticProperty;
}

/**
 * @param {number} value
 * @return {void}
 * @public
 */
function setInterfaceStaticProperty(value) {
  JsInterfaceStaticMethod.staticProperty = value;
}

/**
 * @return {number}
 * @public
 */
function callInterfaceStaticField() {
  return JsInterfaceStaticMethod.STATIC_FIELD;
}

/**
 * @param {!JsInterfaceAccidentalImpl} impl
 * @return {number}
 * @public
 */
function callAccidentalMethod(impl) {
  return impl.interfaceMethod();
}

/**
 * @param {!JsInterfaceAccidentalDefaultMethodImpl} impl
 * @return {number}
 * @public
 */
function callAccidentalDefaultMethod(impl) {
  return impl.m();
}

/**
 * @return {!SomeJsType}
 */
function createJsSubtype() {
  return new class extends SomeJsType {}(123);
}

/**
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
function callEntryPointAdd(a, b) {
  return globalThis['wasmExports']['entryPointAdd'](a, b);
}

/**
 * @return {number}
 */
function callEntryPointWithJsType() {
  const instance = new SomeJsType(123);
  return globalThis['wasmExports']['entryPointWithJsType'](instance);
}

/**
 * @return {number}
 */
function callJsMethodEntryPointWithJsType() {
  const instance = new SomeJsType(123);
  return globalThis['wasmExports']['staticMethod'](instance);
}

/**
 * @return {boolean}
 */
function callEntryPointWithNullJsFunction() {
  return globalThis['wasmExports']['entryPointWithJsFunction'](null);
}

/**
 * @return {boolean}
 */
function callEntryPointWithUndefinedJsFunction() {
  return globalThis['wasmExports']['entryPointWithJsFunction'](undefined);
}

/**
 * @param {!SomeJsType} someJsType
 * @param {!MyNativeType} nativeType
 * @param {?Long} boxedLong
 * @return {?Long}
 * @public
 */
function callMethodWithNativeAndLong(someJsType, nativeType, boxedLong) {
  return someJsType.methodWithNativeAndLong(nativeType, boxedLong);
}

/**
 * @return {!NativeJsTypeConsumer}
 * @public
 */
function newNativeJsTypeConsumer() {
  return new NativeJsTypeConsumer();
}

/**
 * @param {!NativeJsTypeConsumer} consumer
 * @param {!MyNativeType} nativeType
 * @return {number}
 * @public
 */
function callConsumerGetValue(consumer, nativeType) {
  return consumer.callGetValue(nativeType);
}

/**
 * @param {!NativeJsTypeConsumer} consumer
 * @param {!MyNativeType} nativeType
 * @param {number} delta
 * @return {number}
 * @public
 */
function callConsumerAdd(consumer, nativeType, delta) {
  return consumer.callAdd(nativeType, delta);
}

/**
 * @param {!NativeJsTypeConsumer} consumer
 * @param {!MyNativeType} nativeType
 * @return {number}
 * @public
 */
function callConsumerGetField(consumer, nativeType) {
  return consumer.callGetField(nativeType);
}

/**
 * @param {!NativeJsTypeConsumer} consumer
 * @param {!MyNativeType} nativeType
 * @param {?Long} boxedLong
 * @return {?Long}
 * @public
 */
function callConsumerCombineWithLong(consumer, nativeType, boxedLong) {
  return consumer.callCombineWithLong(nativeType, boxedLong);
}

/**
 * @param {!Long} a
 * @param {!Long} b
 * @return {!Long}
 * @public
 */
function callEntryPointAddLong(a, b) {
  return globalThis['wasmExports']['entryPointAddLong'](a, b);
}

/**
 * @return {boolean}
 * @public
 */
function testDirectEntryPointAddLongFromJs() {
  const a = Long.fromString('1111111111111111111');
  const b = Long.fromString('2222222222222222222');
  const result = globalThis['wasmExports']['entryPointAddLong'](a, b);
  return result instanceof Long && result.toString() === '3333333333333333333';
}

/**
 * @return {function(?): ?}
 * @public
 */
function getFunctionWithObject() {
  return (a) => a;
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
 * @return {function(?SomeJsType): ?SomeJsType}
 * @public
 */
function getFunctionWithJsType() {
  return (a) => a;
}

/**
 * @param {function(?SomeJsType): ?SomeJsType} fn
 * @param {?SomeJsType} a
 * @return {?SomeJsType}
 * @public
 */
function callFunctionWithJsType(fn, a) {
  return fn(a);
}

/**
 * @return {!SimpleJsEnum}
 * @public
 */
function getSimpleJsEnumOne() {
  return SimpleJsEnum.ONE;
}

/**
 * @return {!SimpleJsEnum}
 * @public
 */
function getSimpleJsEnumTwo() {
  return SimpleJsEnum.TWO;
}

/**
 * @return {!SimpleJsEnum}
 * @public
 */
function getSimpleJsEnumThree() {
  return SimpleJsEnum.THREE;
}

/**
 * @return {boolean}
 * @public
 */
function checkJsEnumEquality() {
  return SimpleJsEnum.ONE === SimpleJsEnum.ONE && SimpleJsEnum.ONE !== SimpleJsEnum.TWO;
}

/**
 * @param {!SimpleJsEnum} e
 * @return {!SimpleJsEnum}
 * @public
 */
function passThroughJsEnum(e) {
  return e;
}

/**
 * @return {!StringValuedJsEnum}
 * @public
 */
function getStringValuedJsEnumFoo() {
  return StringValuedJsEnum.FOO;
}

/**
 * @return {!StringValuedJsEnum}
 * @public
 */
function getStringValuedJsEnumBar() {
  return StringValuedJsEnum.BAR;
}

/**
 * @return {!IntValuedJsEnum}
 * @public
 */
function getIntValuedJsEnumMinusTen() {
  return IntValuedJsEnum.MINUS_TEN;
}

/**
 * @return {!IntValuedJsEnum}
 * @public
 */
function getIntValuedJsEnumTen() {
  return IntValuedJsEnum.TEN;
}

/**
 * @param {!SimpleJsEnum} e
 * @return {string}
 * @public
 */
function switchOnSimpleJsEnum(e) {
  switch (e) {
    case SimpleJsEnum.ONE:
      return "1-from-js";
    case SimpleJsEnum.TWO:
      return "2-from-js";
    case SimpleJsEnum.THREE:
      return "3-from-js";
    default:
      return 'unknown';
  }
}

/**
 * @param {!IntValuedJsEnum} e
 * @return {string}
 * @public
 */
function switchOnIntValuedJsEnum(e) {
  switch (e) {
    case IntValuedJsEnum.MINUS_TEN:
      return "-10-from-js";
    case IntValuedJsEnum.TEN:
      return "10-from-js";
    default:
      return 'unknown';
  }
}

/**
 * @param {!StringValuedJsEnum} e
 * @return {string}
 * @public
 */
function switchOnStringValuedJsEnum(e) {
  switch (e) {
    case StringValuedJsEnum.FOO:
      return 'foo-from-js';
    case StringValuedJsEnum.BAR:
      return 'bar-from-js';
    default:
      return 'unknown';
  }
}

exports = {
  newBaseJsType,
  newSomeJsType,
  callGetNumber,
  callGetNumberViaStaticMethod,
  callGetString,
  callGetLong,
  callGetNativeJsType,
  getField,
  setField,
  getStaticField,
  setStaticField,
  getReadOnlyField,
  getStaticReadOnlyField,
  getStaticFieldSameJsNameAsInstanceField,
  getReadOnlyProperty,
  getStaticReadOnlyProperty,
  getStaticPropertySameJsNameAsInstanceProperty,
  getReadWriteProperty,
  setReadWriteProperty,
  callPackagePrivateMethod,
  callReturnSelf,
  callTakesSelf,
  callMethodWithTypeParameters,
  newCapturesOuter,
  callGetOuter,
  callAbstractMethod,
  callInterfaceMethod,
  callInterfaceGetNumber,
  callInterfaceRenamedMethod,
  callInterfaceDefaultMethod,
  callInterfaceStaticMethod,
  callInterfaceStaticProperty,
  setInterfaceStaticProperty,
  callInterfaceStaticField,
  callAccidentalMethod,
  callAccidentalDefaultMethod,
  createJsSubtype,
  callEntryPointAdd,
  callEntryPointWithJsType,
  callJsMethodEntryPointWithJsType,
  callEntryPointWithNullJsFunction,
  callEntryPointWithUndefinedJsFunction,
  newNativeJsTypeConsumer,
  callConsumerAdd,
  callConsumerCombineWithLong,
  callConsumerGetField,
  callConsumerGetValue,
  callMethodWithNativeAndLong,
  callGetPrimitiveLong,
  callAddPrimitiveLong,
  callEntryPointAddLong,
  testDirectEntryPointAddLongFromJs,
  getFunctionWithObject,
  callFunctionWithObject,
  getFunctionWithJsType,
  callFunctionWithJsType,
  getLongField,
  setLongField,
  getSimpleJsEnumOne,
  getSimpleJsEnumTwo,
  getSimpleJsEnumThree,
  checkJsEnumEquality,
  passThroughJsEnum,
  getStringValuedJsEnumFoo,
  getStringValuedJsEnumBar,
  getIntValuedJsEnumMinusTen,
  getIntValuedJsEnumTen,
  switchOnSimpleJsEnum,
  switchOnIntValuedJsEnum,
  switchOnStringValuedJsEnum,
};
