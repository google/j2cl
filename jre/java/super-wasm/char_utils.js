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

goog.module('j2wasm.CharUtils');


/**
 * @param {number} value
 * @return {number}
 */
function codePointToLowerCase(value) {
  return String.fromCodePoint(value).toLowerCase().codePointAt(0);
}

/**
 * @param {number} value
 * @return {number}
 */
function codePointToUpperCase(value) {
  return String.fromCodePoint(value).toUpperCase().codePointAt(0);
}

/**
 * @param {number} value
 * @return {number}
 */
function charToLowerCase(value) {
  return String.fromCharCode(value).toLowerCase().charCodeAt(0);
}

/**
 * @param {number} value
 * @return {number}
 */
function charToUpperCase(value) {
  return String.fromCharCode(value).toUpperCase().charCodeAt(0);
}

exports = {
  codePointToLowerCase,
  codePointToUpperCase,
  charToLowerCase,
  charToUpperCase,
};
