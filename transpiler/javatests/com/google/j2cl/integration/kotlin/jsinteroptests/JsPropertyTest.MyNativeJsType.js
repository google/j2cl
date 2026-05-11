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

goog.module('jsinteroptests.JsPropertyTest.MyNativeJsType');

class MyNativeJsType {
  /** @param {number=} x */
  constructor(x) {
    /** @public {number} */
    this.x = x || 0;
    /** @public {number} */
    this.y = 0;
    /** @public {boolean} */
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
}

/**
 * @public {number}
 */
MyNativeJsType.staticX = 33;

exports = MyNativeJsType;
