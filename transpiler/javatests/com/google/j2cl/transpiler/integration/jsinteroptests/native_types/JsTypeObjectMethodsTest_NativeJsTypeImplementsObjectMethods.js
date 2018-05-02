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
goog.module('woo.JsTypeObjectMethodsTest.NativeJsTypeImplementsObjectMethods');

class NativeJsTypeImplementsObjectMethods {
  /** @param {number} value */
  constructor(value) {
    /** public {number} */ this.value = value;
  }

  /**
   * @param {?} other
   * @return {boolean}
   */
  equals(other) {
    return this.value === other.value;
  }

  /** @return {number} */
  hashCode() {
    return this.value;
  }

  /** @return {string} */
  toString() {
    return 'Native Object with value: ' + this.value;
  }
}

exports = NativeJsTypeImplementsObjectMethods;
