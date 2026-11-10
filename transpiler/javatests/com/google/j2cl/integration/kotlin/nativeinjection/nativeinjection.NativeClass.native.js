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
 * Replaces the native instance method 'nativeInstanceMethod' in NativeClass.
 * @param {number} a
 * @param {number} b
 * @return {string}
 * @public
 */
NativeClass.prototype.nativeInstanceMethod = function(a, b) {
  return 'nativeInstanceMethod' + (a + b);
};

/** @public {string} */
NativeClass.prototype.nativeInstanceProperty = 'nativeInstanceProperty';

/**
 * @return {number}
 * @public
 */
NativeClass.prototype.callsInstanceMember = function() {
  return this.instanceMember() + 1;
};

/**
 * Replaces the native static method 'nativeStaticMethod' in NativeClass.
 * @param {number} a
 * @param {number} b
 * @return {string}
 * @public
 */
NativeClass.nativeStaticMethod = function(a, b) {
  return 'nativeStaticMethod' + (a + b);
};

/**
 * @return {string}
 * @public
 */
nativeinjection_NativeClass.nativeStaticMethodUsingLongAlias = function() {
  return 'nativeStaticMethodUsingLongAlias';
};

/** @public {number} */
NativeClass.nativeStaticProperty = 42;

/**
 * @param {number} value
 * @return {number}
 * @public
 */
NativeClass.callsStaticMember = function(value) {
  return NativeClass.staticMember(value);
};

/** @public {number} */
NativeClass.nativeInitializationCount = (NativeClass.nativeInitializationCount || 0) + 1;

/**
 * @return {number}
 * @public
 */
NativeClass.callsConstructor = function() {
  return new NativeClass().instanceMember();
};
