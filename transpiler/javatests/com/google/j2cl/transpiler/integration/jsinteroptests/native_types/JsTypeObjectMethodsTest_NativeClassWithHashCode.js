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
 * @fileoverview Native type with hashCode.
 */

goog.module('woo.JsTypeObjectMethodsTest.NativeClassWithHashCode');

class NativeClassWithHashCode {
  /** @param {number=} value */
  constructor(value) {
    /** @public {number} */
    this.myValue = value || 0;
  }
  /** @return {number} */
  hashCode() {
    return this.myValue;
  }

  /**
   * @return {string}
   * @override
   */
  toString() {
    return 'myValue: ' + this.myValue;
  }

  /**
   *  @param {?} other
   *  @return {boolean}
   */
  equals(other) {
    return this.myValue === other.myValue;
  }
}

exports = NativeClassWithHashCode;
