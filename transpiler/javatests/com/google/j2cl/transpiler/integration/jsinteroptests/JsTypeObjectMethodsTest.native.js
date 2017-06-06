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
 * @param {number} a
 * @param {number} b
 * @return {*}
 */
JsTypeObjectMethodsTest.createWithEqualsAndHashCode = function(a, b) {
  return {
    a: a,
    b: b,
    hashCode: function() { return this.b; },
    equals: function(other) { return this.a == other.a; }
  };
};

/**
 * @param {number} a
 * @param {number} b
 * @return {*}
 */
JsTypeObjectMethodsTest.createWithoutEqualsAndHashCode = function(a, b) {
  return {a: a, b: b};
};

/**
 * @param {*} a
 * @return {*}
 */
JsTypeObjectMethodsTest.callHashCode = function(a) {
  return a.hashCode();
};

/**
 * @return {void}
 */
JsTypeObjectMethodsTest.patchErrorWithJavaLangObjectMethods = function() {
  Error.prototype.equals = function(o) { return this.myValue == o.myValue; };
};
