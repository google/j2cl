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

goog.module('j2wasm.ExceptionUtils');

/** @type {!WebAssembly.Tag} */
const tag = new WebAssembly.Tag({parameters: ['externref']});

/**
 * @param {?} param
 * @param {string} message
 * @return {!WebAssembly.Exception}.
 */
function create(param, message) {
  const e = new WebAssembly.Exception(tag, [param], {traceStack: true});
  // Message is not a standard property on WebAssembly.Exception
  e["message"] = message;
  return e;
}

/**
 * @param {!WebAssembly.Exception} e
 */
function throwException(e) {
  throw e;
}

exports = {
  tag,
  create,
  throwException
};
