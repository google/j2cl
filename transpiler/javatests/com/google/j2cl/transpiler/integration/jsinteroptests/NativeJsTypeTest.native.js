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

let _NativeJsTypeWithOverlay = goog.require('woo.NativeJsTypeTest.NativeJsTypeWithOverlay');

/**
 * @return {_NativeJsTypeWithOverlay}
 */
NativeJsTypeTest.createNativeJsTypeWithOverlayWithM = function() {
  let subtypeWithM = class extends _NativeJsTypeWithOverlay {
    constructor() {
      super();
      /** @type {?} */ (this)['m'] = function() {
        return 6;
      };
    }
  };
  return new subtypeWithM();
};

/**
 * @return {*}
 */
NativeJsTypeTest.createNativeObjectWithToString = function() {
  return {toString: function() { return 'Native type'; }};
};

/**
 * @return {*}
 */
NativeJsTypeTest.createNativeObject = function() {
  return {};
};

/**
 * @return {*}
 */
NativeJsTypeTest.createNativeArray = function() {
  return [];
};

/**
 * @return {*}
 */
NativeJsTypeTest.createFunction = function() {
  return function() {};
};

/**
 * @return {*}
 */
NativeJsTypeTest.createNumber = function() {
  return 42;
};

/**
 * @return {*}
 */
NativeJsTypeTest.createBoxedNumber = function() {
  return new Number(42);
};

/**
 * @return {*}
 */
NativeJsTypeTest.createBoxedString = function() {
  return new String('hello');
};

/**
 * @return {*}
 */
NativeJsTypeTest.createNativeSubclass = function() {
  const implementingClass = class {
    constructor() {
      /** @private {?string} */
      this.element_ = undefined;
    }
    /** @param {string} e */
    add(e) {
      this.element_ = e;
    }
    /**
     * @param {string} e
     * @return {boolean}
     */
    remove(e) {
      let ret = this.element_ == e;
      this.element_ = undefined;
      return ret;
    }
  };
  return new implementingClass();
};
