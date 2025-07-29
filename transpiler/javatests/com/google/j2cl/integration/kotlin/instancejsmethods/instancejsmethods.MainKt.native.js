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
MainKt.callParentFun = function(p, a, b) {
  MainKt.$clinit();
  return p.sum(a, b);
};

/**
 * @param {*} p
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
MainKt.callParentBar = function(p, a, b) {
  MainKt.$clinit();
  return p.bar(a, b);
};

/**
 * @param {*} p
 * @param {number} a
 * @return {number}
 */
MainKt.callParentFoo = function(p, a) {
  MainKt.$clinit();
  return p.myFoo(a);
};

/**
 * @param {*} c
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
MainKt.callChildFun = function(c, a, b) {
  MainKt.$clinit();
  return c.sum(a, b);
};

/**
 * @param {*} c
 * @param {number} a
 * @param {number} b
 * @return {number}
 */
MainKt.callChildBar = function(c, a, b) {
  MainKt.$clinit();
  return c.bar(a, b);
};

/**
 * @param {*} c
 * @param {number} a
 * @return {number}
 */
MainKt.callChildFoo = function(c, a) {
  MainKt.$clinit();
  return c.myFoo(a);
};

/**
 * @param {*} c
 * @param {number} a
 * @return {number}
 */
MainKt.callChildIntfFoo = function(c, a) {
  MainKt.$clinit();
  return c.intfFoo(a);
};
