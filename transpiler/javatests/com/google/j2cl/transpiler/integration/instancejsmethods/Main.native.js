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
 * @param {*} p
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
Main.callParentFun = function(p, a, b) {
  Main.$clinit();
  return p.sum(a, b);
};

/**
 * @param {*} p
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
Main.callParentBar = function(p, a, b) {
  Main.$clinit();
  return p.bar(a, b);
};

/**
 * @param {*} p
 * @param {number} a
 * @return {number}
 */
Main.callParentFoo = function(p, a) {
  Main.$clinit();
  return p.myFoo(a);
};

/**
 * @param {*} c
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
Main.callChildFun = function(c, a, b) {
  Main.$clinit();
  return c.sum(a, b);
};

/**
 * @param {*} c
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
Main.callChildBar = function(c, a, b) {
  Main.$clinit();
  return c.bar(a, b);
};

/**
 * @param {*} c
 * @param {number} a
 * @return {number}
 */
Main.callChildFoo = function(c, a) {
  Main.$clinit();
  return c.myFoo(a);
};

/**
 * @param {*} c
 * @param {number} a
 * @return {number}
 */
Main.callChildIntfFoo = function(c, a) {
  Main.$clinit();
  return c.intfFoo(a);
};
