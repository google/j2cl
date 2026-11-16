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
goog.module('test.NativeVarargs');

class NativeVarargs {
  /** @param {...?NativeVarargs} items */
  constructor(...items) {
    this.length = items.length;
  }

  /** @param {...?NativeVarargs} items */
  accept(...items) {
    this.length += items.length;
  }

  /**
   * @param {number} prefix
   * @param {...?NativeVarargs} items
   */
  acceptWithPrefix(prefix, ...items) {
    this.length += prefix + items.length;
  }

  /**
   * @return {number}
   */
  getLength() {
    return this.length;
  }

  /**
   * @param {...?NativeVarargs} items
   * @return {?NativeVarargs}
   */
  static of(...items) {
    return new NativeVarargs(...items);
  }
}

exports = NativeVarargs;
