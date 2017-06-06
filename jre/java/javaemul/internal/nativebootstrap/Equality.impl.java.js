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
 * Impl hand rolled.
 */
goog.module('nativebootstrap.Equality$impl');


/**
 * Miscellaneous equality functions.
 */
class Equality {
  /**
   * Strict equality that combines undefined and null.
   *
   * @param {*} left
   * @param {*} right
   * @return {boolean}
   */
  static $same(left, right) {
    return left === right || (left == null && right == null);
  }
};

// Ensure Equality.$same() is not inlined so J2clEqualitySameRewriterPass can
// detect all calls.
// TODO(goktug): we should have a better way of doing this.
goog.global['_$same'] = Equality.$same;

/**
 * Exported class.
 */
exports = Equality;
