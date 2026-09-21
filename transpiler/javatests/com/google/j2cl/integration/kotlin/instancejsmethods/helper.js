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

goog.module('instancejsmethods.helper');

const Child = goog.require('instancejsmethods.Child');
const Parent = goog.require('instancejsmethods.Parent');

/**
 * @param {!Parent} p
 * @param {number} a
 * @param {number} b
 * @return {number}
 * @public
 */
exports.callParentFun = function(p, a, b) {
  return p.sum(a, b);
};

/**
 * @param {!Parent} p
 * @param {number} a
 * @param {number} b
 * @return {number}
 * @public
 */
exports.callParentBar = function(p, a, b) {
  return p.bar(a, b);
};

/**
 * @param {!Parent} p
 * @param {number} a
 * @return {number}
 * @public
 */
exports.callParentFoo = function(p, a) {
  return p.myFoo(a);
};

/**
 * @param {!Child} c
 * @param {number} a
 * @param {number} b
 * @return {number}
 * @public
 */
exports.callChildFun = function(c, a, b) {
  return c.sum(a, b);
};

/**
 * @param {!Child} c
 * @param {number} a
 * @param {number} b
 * @return {number}
 * @public
 */
exports.callChildBar = function(c, a, b) {
  return c.bar(a, b);
};

/**
 * @param {!Child} c
 * @param {number} a
 * @return {number}
 * @public
 */
exports.callChildFoo = function(c, a) {
  return c.myFoo(a);
};

/**
 * @param {!Child} c
 * @param {number} a
 * @return {number}
 * @public
 */
exports.callChildIntfFoo = function(c, a) {
  return c.intfFoo(a);
};
