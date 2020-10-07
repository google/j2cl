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
 * @return {?number}
 */
Main.callSumAndMultiply = function() {
  return Main.sumAndMultiply(10, 1, 2);
};

/**
 * @return {number}
 */
Main.callF1 = function() {
  return Main.f1(10, 1, 2);
};

/**
 * @return {number}
 */
Main.callF2 = function() {
  return Main.f2(1, 2);
};

/**
 * @param {Main} m
 * @return {number}
 */
Main.callF3 = function(m) {
  return m.f3(10, 1, 2);
};

/**
 * @param {Main} m
 * @return {number}
 */
Main.callF4 = function(m) {
  return m.f4(1, 2);
};

/**
 * @param {*} a
 * @return {number}
 */
Main.callJsFunction = function(a) {
  return (/**@type {Function}*/ (a))(0, null, null);
};
