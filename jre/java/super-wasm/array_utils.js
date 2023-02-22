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

// TODO(b/193532287) Re-evaluate these when array interop is supported.
goog.module('j2wasm.ArrayUtils');


/**
 * @param {number} size
 * @return {!Array<*>}
 */
function createBuffer(size) {
  return new Array(size);
}

/**
 * @param {!Array<*>} buffer
 * @param {number} index
 * @param {*} value
 */
function setBufferAt(buffer, index, value) {
  buffer[index] = value;
}

/**
 * @param {!Array<*>} buffer
 * @param {number} index
 * @return {*}
 */
function getBufferAt(buffer, index) {
  return buffer[index];
}

/**
 * @param {!Array<*>} buffer
 * @return {number}
 */
function getLength(buffer) {
  return buffer.length;
}

exports = {
  createBuffer,
  setBufferAt,
  getBufferAt,
  getLength,
};
