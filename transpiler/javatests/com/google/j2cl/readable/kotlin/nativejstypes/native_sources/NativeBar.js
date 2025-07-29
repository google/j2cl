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
goog.module('nativejstypes.Bar');

class Bar {
  /**
   * @param {number} a
   * @param {number} b
   */
  constructor(a, b) {
    /** @public {number} */
    this.x = a;
    /** @public {number} */
    this.y = b;
  }

  product() { return this.x * this.y; }

  static getStatic() { return 1; }
};

/**
 * @public {number}
 */
Bar.f = 1;

Bar.Inner = class {
  /** @param {number} n */
  constructor(n) {
    /** @public {number} */
    this.n = n;
  }

  square() { return this.n * this.n; }

  static getInnerStatic() { return 2; }
};

exports = Bar;
