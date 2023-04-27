// Copyright 2023 Google Inc.
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

goog.module('j2wasm.StringUtils');

/**
 * @param {string} a
 * @param {string} b
 * @return {boolean}
 */
function equalsIgnoreCase(a, b) {
  return a.toLowerCase() == b.toLowerCase();
}

/**
 * @param {string} a
 * @param {string} b
 * @return {number}
 */
function compareToIgnoreCase(a, b) {
  a = a.toLowerCase();
  b = b.toLowerCase();
  return a == b ? 0 : (a < b) ? -1 : 1;
}

exports = {
  equalsIgnoreCase,
  compareToIgnoreCase
};
