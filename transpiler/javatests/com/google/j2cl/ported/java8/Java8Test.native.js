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
 * @return {*}
 * @public
 */
Java8Test
    .createNativeJsTypeInterfaceWithStaticInitializationAndInstanceOverlayMethod =
    function() {
  return {
    getA: function() {
      return 1;
    }
  };
};


/**
 * @param {?function(number, ...?string):?string} f
 * @return {?string}
 * @public
 */
Java8Test.callFromJSNI = function(f) {
  return f(2, 'a', 'b', 'c');
};


/**
 * @return {?function():?number}
 * @public
 */
Java8Test.createNative = function() {
  return function() {
    return 5;
  };
};
