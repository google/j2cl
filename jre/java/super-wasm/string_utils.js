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

// TODO(b/272381112): Remove after non-stringref experiment.
/**
 * @param {!Object} wasmArray
 * @param {number} start
 * @param {number} end
 * @return {string}
 */
function stringNew(wasmArray, start, end) {
  const charArrayGet = goog.global['j2wasm_exports']['charArrayGet'];

  let result = '';

  let buffer;
  do {
    const bufferSize = Math.min(end - start, 1000);
    if (buffer == null || buffer.length != bufferSize) {
      buffer = new Array(bufferSize);
    }
    for (let i = 0; i < bufferSize; i++) {
      buffer[i] = charArrayGet(wasmArray, start + i);
    }
    result += String.fromCharCode.apply(null, buffer);
    start += bufferSize;
  } while (start < end);

  return result;
}

// TODO(b/272381112): Remove after non-stringref experiment.
/**
 * @param {string} string
 * @param {!Object} wasmArray
 * @param {number} start
 */
function stringGetChars(string, wasmArray, start) {
  const charArraySet = goog.global['j2wasm_exports']['charArraySet'];
  for (let i = 0; i < string.length; i++) {
    charArraySet(wasmArray, start + i, string.charCodeAt(i));
  }
}

// TODO(b/272381112): Remove after non-stringref experiment.
/**
 * @param {string} string
 * @return {number}
 */
function stringLength(string) {
  return string.length;
}

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
  compareToIgnoreCase,
  stringNew,
  stringGetChars,
  stringLength
};
