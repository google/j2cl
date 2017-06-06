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
goog.module('test.foo.JsPropertyTest_MyNativeJsType');

class JsPropertyTest_MyNativeJsType {
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
  static answerToLife() { return 42; }

  /**
   * @param {number} bias
   * @return {number}
   * @public
   */
  sum(bias) { return this.x + bias; }
};

/**
 * @public {number}
 */
JsPropertyTest_MyNativeJsType.staticX = 33;

exports = JsPropertyTest_MyNativeJsType;
