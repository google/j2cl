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
 * @return {number}
 */
Main.getFooA = function() {
  Main.$clinit();
  return Foo.a;
};

/**
 * @return {number}
 */
Main.getFooB = function() {
  Main.$clinit();
  return Foo.abc;
};

/**
 * @param {*} bar
 * @return {number}
 */
Main.getBarA = function(bar) {
  Main.$clinit();
  return bar.a;
};

/**
 * @param {*} bar
 * @return {number}
 */
Main.getBarB = function(bar) {
  Main.$clinit();
  return bar.abc;
};

/**
 * @param {number} x
 */
Main.setFooA = function(x) {
  Main.$clinit();
  Foo.a = x;
};

/**
 * @param {number} x
 */
Main.setFooB = function(x) {
  Main.$clinit();
  Foo.abc = x;
};

/**
 * @param {*} bar
 * @param {number} x
 */
Main.setBarA = function(bar, x) {
  Main.$clinit();
  bar.a = x;
};

/**
 * @param {*} bar
 * @param {number} x
 */
Main.setBarB = function(bar, x) {
  Main.$clinit();
  bar.abc = x;
};
