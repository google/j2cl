// Copyright 2021 Google Inc.
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

goog.module('j2wasm');


/**
 * Instantiates a web assembly module passing the necessary imports and any
 * additional import the user might need to provide for their application.
 *
 * @param {!BufferSource} moduleObject
 * @param {?Object<string, !Function>=} userImports
 * @return {!Promise<!WebAssembly.Instance>}
 */
async function instantiate(moduleObject, userImports) {
  const jreImports = {
    'Math.random': Math.random,
    'Date.now': Date.now,
    // The following are declared in the JRE but unimplemented for now.
    'Date.UTC': unimplemented,
    'Date.parse': unimplemented,
    'Error.captureStackTrace': unimplemented,
    'Object.getPrototypeOf': unimplemented,
    'Object.keys': unimplemented,
    'Object.values': unimplemented,
    'parseInt': unimplemented,
    'parseFloat': unimplemented,
    'typeof': unimplemented,
  };

  const {instance} = await WebAssembly.instantiate(moduleObject, {
    // Pass the imports required by the jre plus the additional provided by the
    // user. The user imports will override the jre imports if they provide
    // the same key.
    'imports': Object.assign({}, jreImports, userImports)
  });
  return instance;
}

/** @return {void} */
function unimplemented() {
  throw new Error('Unimplemented extern');
}

exports = {instantiate};
